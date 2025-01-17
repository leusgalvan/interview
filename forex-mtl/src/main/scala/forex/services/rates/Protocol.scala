package forex.services.rates

import cats.effect.Sync
import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

import java.time.OffsetDateTime

object Protocol {
  implicit val configuration: Configuration = Configuration.default

  sealed trait OneFrameResponse
  final case class RatesResponse(rates: List[OneFrameRate]) extends OneFrameResponse
  final case class ErrorResponse(error: String) extends OneFrameResponse

  final case class OneFrameRate(
    from: String,
    to: String,
    price: BigDecimal,
    time_stamp: OffsetDateTime
  )

  implicit val oneFrameRateDecoder: Decoder[OneFrameRate] =
    deriveConfiguredDecoder[OneFrameRate]

  implicit val ratesResponseDecoder: Decoder[RatesResponse] =
    Decoder.decodeList(oneFrameRateDecoder).map(RatesResponse.apply)

  implicit val errorResponseDecoder: Decoder[ErrorResponse] =
    deriveConfiguredDecoder[ErrorResponse]

  implicit def oneFrameResponseDecoder: Decoder[OneFrameResponse] = {
    ratesResponseDecoder.either(errorResponseDecoder).map(_.fold[OneFrameResponse](x => x, x => x))
  }

  implicit def jsonDecoder[A : Decoder, F[_]: Sync]: EntityDecoder[F, A] = jsonOf[F, A]
}