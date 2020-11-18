import scala.util.Try
import cats.effect.Sync
import cats.implicits._

object FindUsage {
  private val regex = raw"data-perc='(\d+)'".r.unanchored
  def apply(html: String): Option[Int] = html match {
    case regex(perc) => Try(perc.toInt).toOption
    case _           => None
  }

  def inStream[F[_]: Sync](payload: fs2.Stream[F, String]): F[Option[Int]] = {
    payload.zipWithPrevious
      .map {
        case (Some(previous), current) => s"$previous$current"
        case (None, current)           => current
      }
      .map(FindUsage.apply)
      .collectFirst { case Some(value) => value }
      .head
      .compile
      .toList
      .map(_.headOption)
  }
}
