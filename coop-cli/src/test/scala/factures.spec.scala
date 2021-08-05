import cats.effect.{IO, SyncIO}
import munit.CatsEffectSuite
import cats.effect.Resource
import scala.io.Source
import cats.implicits._
import cats.effect.Blocker
import scala.io.BufferedSource
import java.nio.file.Paths

class FacturesSpec extends CatsEffectSuite {
  val readHtml: IO[String] = {
    val resource = for {
      b <- Blocker[IO]
      src <- Resource.make(b.delay[IO, BufferedSource](Source.fromResource("factures.html")))(src =>
        b.delay[IO, Unit](src.close())
      )
    } yield src
    resource.use(_.getLines().mkString("").pure[IO])
  }

  test("find the factures") {
    readHtml.map { html =>
      assertEquals(
        Factures(html).size,
        7
      )
    }
  }

  val htmlStream: fs2.Stream[IO, String] = {
    val resource = for {
      b <- fs2.Stream.resource(Blocker[IO])
      src <- fs2.io.file.readAll[IO](Paths.get(getClass.getResource("factures.html").toURI()), b, 1024)
    } yield src
    resource.through(fs2.text.utf8Decode)
  }

  test("find the factures in a html payload stream") {
    Factures
      .inStream(htmlStream)
      .map(it =>
        assertEquals(
          it.size,
          7
        )
      )
  }
}
