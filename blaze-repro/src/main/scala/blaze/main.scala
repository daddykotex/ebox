package blaze

import cats.effect._
import cats.implicits._
import org.http4s.headers.Location
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import scala.util.control.NonFatal
import scala.concurrent.duration._

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    import fs2.Stream._
    val program = for {
      blocker <- resource(Blocker[IO])
      client <- resource(BlazeClientBuilder[IO](blocker.blockingContext).resource)
      web = new Web(client)
      login =
        web
          .login("ignore", "these credentials")
          .map(_ => Option.empty[Throwable])
          .handleError { case NonFatal(ex) => Some(ex) }
      (ex, idx) <- fs2.Stream
        .repeatEval(login)
        .metered(5.seconds)
        .zipWithIndex
        .collectFirst { case (Some(ex), idx) => (ex, idx) }
        .head
      _ <- eval(IO.delay(println(s"Error occurred on $idx attempt.")))
      _ <- raiseError[IO](ex)
    } yield ()
    program.compile.drain.as(ExitCode.Success)
  }
}
