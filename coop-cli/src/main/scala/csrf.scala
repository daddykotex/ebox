import scala.util.Try
import cats.effect.Sync
import cats.implicits._

object FindCSRF {
  private def regex(name: String) =
    raw"""[\s\n\r]*name="__RequestVerificationToken"[\s\n\r]*type="hidden"[\s\n\r]*value="([A-Za-z0-9_-]*)"[\s\n\r]*\/>""".r.unanchored
  def apply(name: String)(html: String): Option[String] = {
    val r = regex(name)
    html match {
      case r(token) => Some(token)
      case _        => None
    }
  }

  def inStream[F[_]: Sync](name: String)(payload: fs2.Stream[F, String]): F[Option[String]] = {
    payload.zipWithPrevious
      .map {
        case (Some(previous), current) => s"$previous$current"
        case (None, current)           => current
      }
      .map(FindCSRF.apply(name))
      .collectFirst { case Some(value) => value }
      .head
      .compile
      .toList
      .map(_.headOption)
  }
}
