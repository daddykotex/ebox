package blaze

import scala.util.Try
import cats.effect.Sync
import cats.implicits._

object FindCSRF {
  private val regex = raw"""name="_csrf_security_token" value="([A-Za-z0-9_-]*)" ?>""".r.unanchored
  def apply(html: String): Option[String] = {
    html match {
      case regex(token) => Some(token)
      case _            => None
    }
  }

  def inStream[F[_]: Sync](payload: fs2.Stream[F, String]): F[Option[String]] = {
    payload.zipWithPrevious
      .map {
        case (Some(previous), current) => s"$previous$current"
        case (None, current)           => current
      }
      .map(FindCSRF.apply)
      .collectFirst { case Some(value) => value }
      .head
      .compile
      .toList
      .map(_.headOption)
  }
}
