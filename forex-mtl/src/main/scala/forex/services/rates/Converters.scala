package forex.services.rates

import forex.domain.Rate.Pair
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.services.rates.errors.Error
import forex.services.rates.errors.Error.{OneFrameLookupFailed, OneFrameMalformedResponse}
import cats.syntax.traverse._

object Converters {
  import Protocol._

  private[rates] implicit class OneFrameResponseOps(val oneFrameResponse: OneFrameResponse) extends AnyVal {
    def asRates: Either[Error, List[Rate]] = {
      def parseCurrency(s: String): Either[Error, Currency] =
        Currency.fromString(s).toRight(OneFrameMalformedResponse(s"Invalid currency: $s"))

      def parseRate(oneFrameRate: OneFrameRate): Either[Error, Rate] = {
        for {
          from <- parseCurrency(oneFrameRate.from)
          to <- parseCurrency(oneFrameRate.to)
          price = Price(oneFrameRate.price)
          timestamp = Timestamp(oneFrameRate.time_stamp)
        } yield Rate(Pair(from, to), price, timestamp)
      }

      oneFrameResponse match {
        case RatesResponse(rates) => rates.traverse(parseRate)
        case ErrorResponse(error) => Left(OneFrameLookupFailed(error))
      }
    }
  }

}
