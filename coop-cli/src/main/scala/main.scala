import cats.effect._
import cats.implicits._

import com.monovore.decline._
import com.monovore.decline.effect._
import org.http4s.client.asynchttpclient.AsyncHttpClient

//Opts
final case class CoopCredentials(email: String, password: String)

// Commands
final case class DownloadLatest(credentials: CoopCredentials)
final case class DownloadMonth(credentials: CoopCredentials, year: Int, month: java.time.Month)

object Main
    extends CommandIOApp(
      name = "coop-cli",
      header = "Simple CLI tool get info on your ebox account",
      version = "0.0.x"
    ) {

  val emailOpts: Opts[String] = Opts.argument[String](metavar = "email")
  val passwordOpts: Opts[String] = Opts.argument[String](metavar = "password")
  val credentialsOpts: Opts[CoopCredentials] = (emailOpts, passwordOpts).mapN(CoopCredentials)

  val yearOpts: Opts[Int] = Opts.option[Int](long = "year", help = "The year you want to download.")
  val monthOpts: Opts[java.time.Month] =
    Opts
      .option[Int](long = "month", help = "The month you want to download.")
      .mapValidated { rawMonth =>
        Either
          .catchNonFatal { java.time.Month.of(rawMonth) }
          .leftMap(_ => "Invalid month.")
          .toValidatedNel
      }

  // Commands
  val getLatestCmd: Opts[DownloadLatest] =
    Opts.subcommand("download-latest", "Returns the PDF bytes for the latest month.") {
      credentialsOpts.map(DownloadLatest)
    }
  val getOneCmd: Opts[DownloadMonth] =
    Opts.subcommand("download-month", "Returns the PDF bytes for a given month in a year.") {
      (credentialsOpts, yearOpts, monthOpts).tupled.map((DownloadMonth.apply _).tupled)
    }

  override def main: Opts[IO[ExitCode]] = {
    def run(f: Web => fs2.Stream[IO, Byte]): IO[ExitCode] = {
      (Blocker[IO], AsyncHttpClient.resource[IO]()).tupled
        .use { case (blocker, client) =>
          f(new Web(client))
            .through(fs2.io.stdout(blocker))
            .compile
            .drain
        }
        .as(ExitCode.Success)
    }

    (getLatestCmd orElse getOneCmd).map {
      case DownloadLatest(credentials) =>
        run(_.downloadLatest(credentials))
      case DownloadMonth(credentials, year, month) =>
        run(_.downloadMonth(year, month, credentials))
    }
  }
}
