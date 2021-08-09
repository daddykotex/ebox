import cats.effect.IO
import cats._
import cats.implicits._
import org.http4s.Request
import org.http4s.Response
import org.http4s.client.Client
import org.http4s._
import org.http4s.client.dsl.io._
import org.http4s.headers._
import org.http4s.Method._
import org.http4s.MediaType
import org.http4s.implicits._
import cats.effect.ContextShift

class Web(client: Client[IO])(implicit cs: ContextShift[IO]) {
  private case class CoopAuth(cookieVerif: String, formVerif: String)
  private type RequestTransformer = Request[IO] => Request[IO]

  private val baseUri = uri"https://www.portailcoopsjb.com"

  private def bodyIfSuccess[G[_], F[_], T](
      response: Response[F]
  )(extract: Response[F] => T)(implicit AE: ApplicativeError[G, Throwable]): G[T] = {
    if (response.status.code <= 300) AE.pure(extract(response))
    else AE.raiseError(new RuntimeException(s"Got ${response.status} when trying to read body."))
  }
  private def ioBodyTextIfSucceed[T](response: Response[IO]) = {
    bodyIfSuccess[IO, IO, fs2.Stream[IO, String]](response)(_.bodyText)
  }

  private def bodyBytesIfSucceed[T](response: Response[IO]) = {
    bodyIfSuccess[fs2.Stream[IO, *], IO, fs2.Stream[IO, Byte]](response)(_.body).flatten
  }

  private val csrfCookieName = "__RequestVerificationToken"
  private val sessionCookieName = ".AspNet.ApplicationCookie"

  private def login(credentials: CoopCredentials): IO[RequestTransformer] = {
    def loginRequest(csrf: String) = POST(
      UrlForm(
        "UserNameLogin" -> credentials.email,
        "PasswordLogin" -> credentials.password,
        csrfCookieName -> csrf,
        "vCodeLog" -> "",
        "btnLoginUserProfil" -> "Se connecter"
      ),
      baseUri / "Compte" / "Connexion"
    )

    def doLogin(request: Request[IO]): IO[String] = {
      client
        .run(request)
        .use { resp =>
          val badStatus = resp.status.code =!= 302
          val location = resp.headers.find(_.name === headers.Location.name).map(_.value)
          val goodLocation = location.exists(_.contains("Contenu/Page/Factures-electriques"))
          val checkResp = IO
            .raiseWhen(badStatus || !goodLocation)(
              new RuntimeException(s"Got ${resp.status} when login. Redirected to ${location.getOrElse("N/A")}")
            )
          checkResp *> extractCookieValueFromName(sessionCookieName, resp)
        }
    }

    def extractCookieValueFromName(name: String, resp: Response[IO]): IO[String] = {
      resp.cookies
        .find(_.name === name)
        .liftTo[IO](new RuntimeException(s"Could not find $name in cookies."))
        .map(_.content)
    }

    val csrfRequest = GET(baseUri / "Compte" / "ConnexionInscription")

    val getCSRFs: IO[CoopAuth] =
      csrfRequest.map(client.run).flatMap { resource =>
        resource.use { resp =>
          val cookieToken: IO[String] = extractCookieValueFromName(csrfCookieName, resp)
          val cookieForm: IO[String] = ioBodyTextIfSucceed(resp)
            .flatMap(FindCSRF.inStream[IO](csrfCookieName))
            .flatMap(_.liftTo[IO](new RuntimeException("Could not find CSRF in HTML.")))
          (cookieToken, cookieForm).parTupled
            .map(CoopAuth.tupled)
        }
      }

    for {
      auth <- getCSRFs
      csrfTransformer = { req: Request[IO] => req.addCookie(csrfCookieName, auth.cookieVerif) }

      session <- loginRequest(auth.formVerif).map(csrfTransformer).flatMap(doLogin)
    } yield {
      val sessionTransformer = { req: Request[IO] => req.addCookie(sessionCookieName, session) }
      csrfTransformer.andThen(sessionTransformer)
    }
  }

  def getDownloadLinks(rt: RequestTransformer): IO[List[String]] = {
    val factureRequest = GET(baseUri / "Contenu" / "Page" / "Factures-electriques")
    factureRequest
      .map(rt)
      .map(client.run)
      .flatMap(_.use { resp =>
        ioBodyTextIfSucceed(resp)
          .flatMap(Factures.inStream[IO])
      })
      .map(_.map(_.replace("&amp;", "&")))
  }

  def download(url: String, rt: RequestTransformer): fs2.Stream[IO, Byte] = {
    for {
      uri <- fs2.Stream(baseUri.withPath(url))
      req <- fs2.Stream.eval(GET(uri))
      resp <- client.stream(rt(req))
      body <- bodyBytesIfSucceed(resp)
    } yield body
  }

  def downloadLatest(credentials: CoopCredentials): fs2.Stream[IO, Byte] = {
    for {
      rt <- fs2.Stream.eval(login(credentials))
      links <- fs2.Stream.eval(getDownloadLinks(rt))
      bytes <- links.headOption match {
        case Some(link) => download(link, rt)
        case _          => fs2.Stream.raiseError[IO](new RuntimeException(s"Could not find a link to download."))
      }
    } yield bytes
  }
}
