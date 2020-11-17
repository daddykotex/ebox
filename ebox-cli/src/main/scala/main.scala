import cats.effect._
import cats.implicits._

import com.monovore.decline._
import com.monovore.decline.effect._

//Opts
final case class Credentials(accountNumber: String, password: String)

// Commands
final case class GetUsage(credentials: Credentials)

object Main
    extends CommandIOApp(
      name = "ebox-cli",
      header = "Simple CLI tool get info on your ebox account",
      version = "0.0.x"
    ) {

  val accountNameOpts: Opts[String] = Opts.argument[String](metavar = "account-number")
  val passwordOpts: Opts[String] = Opts.argument[String](metavar = "password")
  val credentialsOpts: Opts[Credentials] = (accountNameOpts, passwordOpts).mapN(Credentials)

  // Commands
  val getUsageCmd: Opts[GetUsage] =
    Opts.subcommand("get-usage", "Returns a percentage of your bandwidth usage.") {
      credentialsOpts.map(GetUsage)
    }

  override def main: Opts[IO[ExitCode]] =
    getUsageCmd.map { case GetUsage(all) =>
      IO.delay(println("50")).as(ExitCode.Success)
    }
}
