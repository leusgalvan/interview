package forex.services.rates

import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder

import java.time.OffsetDateTime

object Protocol {
  implicit val configuration: Configuration = Configuration.default

  final case class OneFrameResponse(
      rates: List[OneFrameRate]
  )

  final case class OneFrameRate(
      from: String,
      to: String,
      price: Double,
      time_stamp: OffsetDateTime
  )

  implicit val oneFrameRateDecoder: Decoder[OneFrameRate] =
    deriveConfiguredDecoder[OneFrameRate]

  implicit val oneFrameResponseDecoder: Decoder[OneFrameResponse] =
    Decoder.decodeList(oneFrameRateDecoder).map(OneFrameResponse.apply)
}
