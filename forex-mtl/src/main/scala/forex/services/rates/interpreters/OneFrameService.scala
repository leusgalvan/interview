package forex.services.rates.interpreters

import cats.effect.Sync
import cats.implicits._
import forex.domain.Rate
import forex.services.rates.Algebra
import forex.services.rates.Converters._
import forex.services.rates.Protocol.OneFrameResponse
import forex.services.rates.errors.Error
import org.http4s._
import org.http4s.client.Client
import org.http4s.implicits.http4sLiteralsSyntax


class OneFrameService[F[_]](client: Client[F])(implicit
    sync: Sync[F],
    entityDecoder: EntityDecoder[F, OneFrameResponse]
) extends Algebra[F] {
  override def get(pair: Rate.Pair): F[Either[Error, Rate]] = {
    val ratesUri = uri"http://localhost:8080/rates".withQueryParam("pair", pair.show)
    val request = Request[F](uri = ratesUri).withHeaders(Header("token", "10dc303535874aeccc86a8251e6992f5"))
    client.expect[OneFrameResponse](request).map(_.asRate)
  }
}

object OneFrameService {
  def apply[F[_]](client: Client[F])(implicit
    sync: Sync[F],
    entityDecoder: EntityDecoder[F, OneFrameResponse]
  ): Algebra[F] =
    new OneFrameService[F](client)
}