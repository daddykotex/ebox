import cats.implicits._
import munit.FunSuite
import com.monovore.decline.Opts
import com.monovore.decline.Command
import com.monovore.decline.Help
import java.time.Month

class OpsSpec extends FunSuite {
  private def expectOpts[A](opts: Opts[A], command: String)(expected: A): Unit =
    Command("test", "test")(opts).parse(command.split(" ")) match {
      case Left(value)  => fail(s"failed to parse '$command'")
      case Right(value) => assertEquals(value, expected)
    }

  test("year opts") {
    expectOpts(Main.yearOpts, "--year 2020")(2020)
  }

  test("month opts") {
    expectOpts(Main.monthOpts, "--month 1")(Month.JANUARY)
    expectOpts(Main.monthOpts, "--month 02")(Month.FEBRUARY)

    expectOpts(Main.monthOpts, "--month 12")(Month.DECEMBER)
  }
}
