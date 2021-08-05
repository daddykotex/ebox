import cats.effect._
import cats.implicits._

import com.monovore.decline._
import com.monovore.decline.effect._
import org.http4s.client.asynchttpclient.AsyncHttpClient

//Opts
final case class CoopCredentials(accountNumber: String, password: String)

// Commands
final case class DownloadLatest(credentials: CoopCredentials)

object Main
    extends CommandIOApp(
      name = "ebox-cli",
      header = "Simple CLI tool get info on your ebox account",
      version = "0.0.x"
    ) {

  val accountNameOpts: Opts[String] = Opts.argument[String](metavar = "account-number")
  val passwordOpts: Opts[String] = Opts.argument[String](metavar = "password")
  val credentialsOpts: Opts[CoopCredentials] = (accountNameOpts, passwordOpts).mapN(CoopCredentials)

  // Commands
  val getUsageCmd: Opts[DownloadLatest] =
    Opts.subcommand("download-latest", "Returns a percentage of your bandwidth usage.") {
      credentialsOpts.map(DownloadLatest)
    }

  override def main: Opts[IO[ExitCode]] =
    getUsageCmd.map { case DownloadLatest(credentials) =>
      (Blocker[IO], AsyncHttpClient.resource[IO]()).tupled
        .use { case (blocker, client) =>
          val byteStream = new Web(client).downloadLatest(credentials)
          byteStream
            .through(fs2.io.stdout(blocker))
            .compile
            .drain
        }
        .as(ExitCode.Success)

    }
}
