package forex.services.redis

import forex.domain.Rate
import forex.services.redis.errors.Error.RedisMalformedValue
import forex.services.redis.errors._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._

object Protocol {
  val ratesKey = "rates"

  def toRedisValue(rates: List[Rate]): String =
    rates.asJson.noSpaces

  def fromRedisValue(ratesStr: String): Either[Error, List[Rate]] = {
    decode[List[Rate]](ratesStr) match {
      case Right(rates) => Right(rates)
      case Left(err) => Left(RedisMalformedValue(err.getMessage))
    }
  }
}