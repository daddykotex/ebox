import cats.effect._
import cats.implicits._

import com.monovore.decline._
import com.monovore.decline.effect._
import org.http4s.client.blaze.BlazeClientBuilder

//Opts
final case class EboxCredentials(accountNumber: String, password: String)

// Commands
final case class GetUsage(credentials: EboxCredentials)

object Main
    extends CommandIOApp(
      name = "ebox-cli",
      header = "Simple CLI tool get info on your ebox account",
      version = "0.0.x"
    ) {

  val accountNameOpts: Opts[String] = Opts.argument[String](metavar = "account-number")
  val passwordOpts: Opts[String] = Opts.argument[String](metavar = "password")
  val credentialsOpts: Opts[EboxCredentials] = (accountNameOpts, passwordOpts).mapN(EboxCredentials)

  // Commands
  val getUsageCmd: Opts[GetUsage] =
    Opts.subcommand("get-usage", "Returns a percentage of your bandwidth usage.") {
      credentialsOpts.map(GetUsage)
    }

  override def main: Opts[IO[ExitCode]] =
    getUsageCmd.map { case GetUsage(credentials) =>
      Blocker[IO]
        .flatMap(blocker => BlazeClientBuilder[IO](blocker.blockingContext).resource)
        .use(client => new Web(client).getUsage(credentials))
        .flatMap { result => IO.delay(println(result)) }
        .as(ExitCode.Success)

    }
}
