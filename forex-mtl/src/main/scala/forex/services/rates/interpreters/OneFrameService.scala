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

class OneFrameService[F[_]: Sync](client: Client[F], oneFrameHost: String, oneFramePort: Int) extends Algebra[F] {
  private val baseUri: F[Uri] = Sync[F].fromEither(
    Uri
      .fromString(s"http://$oneFrameHost:$oneFramePort")
      .leftMap(e => new Exception(e.message)) // should not happen under proper configuration
  )

  override def get(pair: Rate.Pair): F[Either[Error, Rate]] = {
    for {
      base <- baseUri
      ratesUri = base.withPath("rates").withQueryParam("pair", pair.show)
      request = Request[F](uri = ratesUri).withHeaders(Header("token", "10dc303535874aeccc86a8251e6992f5"))
      result <- client.expect[OneFrameResponse](request).map(_.asRate)
    } yield result
  }
}

object OneFrameService {
  def apply[F[_]: Sync](client: Client[F], oneFrameHost: String, oneFramePort: Int): Algebra[F] =
    new OneFrameService[F](client, oneFrameHost, oneFramePort)
}