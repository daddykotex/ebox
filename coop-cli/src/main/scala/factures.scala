import scala.util.Try
import cats.effect.Sync
import cats.implicits._

object Factures {
  private val regex =
    raw"""<a\s+href="(\/DocumentMembre\/Download[A-Za-z0-9_\-\/\?;&=\.]*)"\s*(?:[\w\d\s"_=]*)\s*>\s+([\w<>\/\s\]\[\+]*)\s+<\/a>""".r.unanchored

  private val accentHelper = "\\p{M}".r

  private val compiledRegex = java.util.regex.Pattern.compile("\\p{M}")
  def apply(html: String): List[(String, String)] = {
    val normalized =
      accentHelper.pattern.matcher(java.text.Normalizer.normalize(html, java.text.Normalizer.Form.NFD)).replaceAll("")
    regex.findAllMatchIn(normalized).toList.map(oneMatch => (oneMatch.group(1), oneMatch.group(2)))
  }

  def inStream[F[_]: Sync](payload: fs2.Stream[F, String]): F[List[(String, String)]] = {
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
