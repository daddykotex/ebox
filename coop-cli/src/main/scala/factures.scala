import scala.util.Try
import cats.effect.Sync
import cats.implicits._

object Factures {
  private val regex =
    raw"""href="(\/DocumentMembre\/Download[A-Za-z0-9_\-\/\?;&=\.]*)"""".r.unanchored
  def apply(html: String): List[String] = {
    regex.findAllMatchIn(html).toList.map(_.group(1))
  }

  def inStream[F[_]: Sync](payload: fs2.Stream[F, String]): F[List[String]] = {
    payload.zipWithPrevious
      .map {
        case (Some(previous), current) => s"$previous$current"
        case (None, current)           => current
      }
      .map(Factures.apply)
      .zipWithPrevious
      .flatMap { case (l1, l2) =>
        fs2.Stream.emits(l2.diff(l1.toList.flatten).distinct)
      }
      .compile
      .toList
  }
}
