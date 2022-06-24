package forex.http
package rates

import forex.domain.Currency.show
import forex.domain.Rate.Pair
import forex.domain._
import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}

import java.time.OffsetDateTime

object Protocol {

  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames

  final case class GetApiRequest(
      from: Currency,
      to: Currency
  )

  final case class GetApiResponse(
      from: Currency,
      to: Currency,
      price: Price,
      timestamp: Timestamp
  )

  final case class ErrorResponse(
      errors: List[String]
  )

  implicit val currencyEncoder: Encoder[Currency] =
    Encoder.instance[Currency] { show.show _ andThen Json.fromString }

  implicit val currencyDecoder: Decoder[Currency] =
    Decoder.decodeString.emap(s => Currency.fromString(s).toRight(s"invalid currency: $s"))

  implicit val pairEncoder: Encoder[Pair] =
    deriveConfiguredEncoder[Pair]

  implicit val pairDecoder: Decoder[Pair] =
    deriveConfiguredDecoder[Pair]

  implicit val rateEncoder: Encoder[Rate] =
    deriveConfiguredEncoder[Rate]

  implicit val rateDecoder: Decoder[Rate] =
    deriveConfiguredDecoder[Rate]

  implicit val responseEncoder: Encoder[GetApiResponse] =
    deriveConfiguredEncoder[GetApiResponse]

  implicit val responseDecoder: Decoder[GetApiResponse] =
    deriveConfiguredDecoder[GetApiResponse]

  implicit val errorResponseEncoder: Encoder[ErrorResponse] =
    deriveConfiguredEncoder[ErrorResponse]

  implicit val errorResponseDecoder: Decoder[ErrorResponse] =
    deriveConfiguredDecoder[ErrorResponse]

  implicit val timestampDecoder: Decoder[Timestamp] =
    Decoder[OffsetDateTime].map(Timestamp.apply)
}
