package forex.services.rates

import forex.domain.Rate.Pair
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.services.rates.errors.Error
import forex.services.rates.errors.Error.OneFrameMalformedResponse

object Converters {
  import Protocol._

  private[rates] implicit class OneFrameResponseOps(val oneFrameResponse: OneFrameResponse) extends AnyVal {
    def asRate: Either[Error, Rate] = {
      def parseCurrency(s: String): Either[Error, Currency] =
        Currency.fromString(s).toRight(OneFrameMalformedResponse(s"Invalid from currency: $s"))

      def parseRate(oneFrameRate: OneFrameRate): Either[Error, Rate] = {
        for {
          from <- parseCurrency(oneFrameRate.from)
          to <- parseCurrency(oneFrameRate.to)
          price = Price(BigDecimal.decimal(oneFrameRate.price))
          timestamp = Timestamp(oneFrameRate.time_stamp)
        } yield Rate(Pair(from, to), price, timestamp)
      }

      lazy val noRateError: Either[Error, Rate] = Left(OneFrameMalformedResponse("Rates array is empty"))

      oneFrameResponse.rates.headOption.fold(noRateError)(parseRate)
    }
  }

}
