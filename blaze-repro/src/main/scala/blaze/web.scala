package blaze

import cats.effect.IO
import cats._
import cats.implicits._
import org.http4s.Request
import org.http4s.Response
import org.http4s.client.Client
import org.http4s._
import org.http4s.client.dsl.io._
import org.http4s.headers._
import org.http4s.MediaType
import org.http4s.Method._
import org.http4s.implicits._
import cats.effect.ContextShift

class Web(client: Client[IO])(implicit cs: ContextShift[IO]) {
  private type RequestTransformer = Request[IO] => Request[IO]

  private val baseUri = uri"https://client.ebox.ca"

  private def bodyIfSuccess(response: Response[IO]): IO[fs2.Stream[IO, String]] = {
    if (response.status.code <= 300) IO.pure(response.bodyText)
    else IO.raiseError(new RuntimeException(s"Got ${response.status} when trying to read body."))
  }

  def login(user: String, password: String): IO[RequestTransformer] = {
    def loginRequest(csrf: String) = POST(
      UrlForm(
        "usrname" -> user,
        "pwd" -> password,
        "_csrf_security_token" -> csrf
      ),
      baseUri / "login"
    )
    def doLogin(request: Request[IO]): IO[Unit] = {
      client
        .run(request)
        .use { _ => IO.unit }
    }

    val csrfRequest = GET(baseUri)
    val getCSRFAndSession: IO[(String, String)] =
      csrfRequest.map(client.run).flatMap { resource =>
        resource.use { resp =>
          val session: IO[String] =
            resp.cookies
              .find(_.name === "PHPSESSID")
              .liftTo[IO](new RuntimeException("Could not find PHPSESSID in cookies."))
              .map(_.content)
          val csrf: IO[String] = bodyIfSuccess(resp)
            .flatMap(FindCSRF.inStream[IO])
            .flatMap(_.liftTo[IO](new RuntimeException("Could not find CSRF in HTML.")))
          (csrf, session).parTupled
        }
      }

    for {
      (csrf, session) <- getCSRFAndSession

      reqTransformer = { req: Request[IO] => req.addCookie("PHPSESSID", session) }

      _ <- loginRequest(csrf).map(reqTransformer).flatMap(doLogin)
    } yield reqTransformer
  }

  def oneRequest[T](path: Uri): IO[(Status, Headers)] = {
    GET(baseUri.withPath(path.path)).flatMap(client.run(_).use(r => (r.status, r.headers).pure[IO]))
  }
}
