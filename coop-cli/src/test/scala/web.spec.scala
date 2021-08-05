import munit.CatsEffectSuite
import cats.effect._
import cats.implicits._
import java.nio.file.Paths
import org.http4s._, org.http4s.dsl.io._, org.http4s.implicits._
import org.http4s.client.Client
import cats._

final case class LoginInfo(username: String, pwd: String, csrf: String)
object LoginInfo {
  implicit val eq: Eq[LoginInfo] = Eq.fromUniversalEquals
}

class WebSpec extends CatsEffectSuite {
  def fromFile(name: String): fs2.Stream[IO, String] = {
    val resource = for {
      b <- fs2.Stream.resource(Blocker[IO])
      src <- fs2.io.file.readAll[IO](Paths.get(getClass.getResource(name).toURI()), b, 1024)
    } yield src
    resource.through(fs2.text.utf8Decode)
  }

  val loginHtml = fromFile("login.html")
  val usageHtml = fromFile("myusage.html")

  def validService(expectedInfo: LoginInfo) = HttpRoutes.of[IO] {
    case GET -> Root =>
      Ok(loginHtml).map(
        _.putHeaders(headers.`Content-Type`(MediaType.text.html))
          .addCookie("PHPSESSID", "some-session")
      )

    case req @ POST -> Root / "login" =>
      req.as[UrlForm].flatMap { body =>
        val maybeInfo = for {
          user <- body.getFirst("usrname").toRight("Missing username")
          pwd <- body.getFirst("pwd").toRight("Missing password")
          csrf <- body.getFirst("_csrf_security_token").toRight("Missing CSRF")
        } yield LoginInfo(user, pwd, csrf)

        maybeInfo
          .leftMap(err => new RuntimeException(err))
          .liftTo[IO]
          .flatMap { info =>
            if (info === expectedInfo) {
              Found(headers.Location(uri"/home")).map(_.addCookie("PHPSESSID", "some-session"))
            } else {
              Found(headers.Location(uri"/?err=CLI=9999")).map(_.addCookie("PHPSESSID", "some-session"))
            }
          }
      }

    case GET -> Root / "myusage" =>
      Ok(usageHtml)
        .map(
          _.withContentType(headers.`Content-Type`(MediaType.text.html))
            .addCookie("PHPSESSID", "some-session")
        )
  }

  test("get CSRF and PHPSESSID, login, then request usage and parse response") {
    // val login = LoginInfo("user", "pwd", "DAxHvBZblgpGttym2oJYDI7mpT6MWArGFuS2TLaaglQ")
    // val client = Client.fromHttpApp(validService(login).orNotFound)
    // new Web(client).getUsage(CoopCredentials(login.username, login.pwd)).map(it => assertEquals(it, 32))
  }

  test("fail if password is wrong") {
    // val login = LoginInfo("user", "pwd", "DAxHvBZblgpGttym2oJYDI7mpT6MWArGFuS2TLaaglQ")
    // val client = Client.fromHttpApp(validService(login).orNotFound)
    // interceptMessageIO[RuntimeException]("Got 302 Found when login (redirected to /?err=CLI%3D9999).")(
    //   new Web(client)
    //     .getUsage(CoopCredentials(login.username, "oops"))
    // )
  }

  test("fail if username is wrong") {
    // val login = LoginInfo("user", "pwd", "DAxHvBZblgpGttym2oJYDI7mpT6MWArGFuS2TLaaglQ")
    // val client = Client.fromHttpApp(validService(login).orNotFound)
    // interceptMessageIO[RuntimeException]("Got 302 Found when login (redirected to /?err=CLI%3D9999).")(
    //   new Web(client).getUsage(CoopCredentials("oops", login.pwd)).map(it => assertEquals(it, 32))
    // )
  }
}
