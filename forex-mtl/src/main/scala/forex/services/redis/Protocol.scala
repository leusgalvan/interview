package forex.services.redis

import forex.domain.Rate
import forex.services.redis.errors.Error.RedisMalformedValue
import forex.services.redis.errors._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._

object Protocol {
  def toRedisKey(pair: Rate.Pair): String =
    s"${pair.from}${pair.to}"

  def toRedisValue(rate: Rate): String =
    rate.asJson.noSpaces

  def fromRedisValue(value: String): Either[Error, Rate] = {
    decode[Rate](value) match {
      case Right(rate) => Right(rate)
      case Left(err) => Left(RedisMalformedValue(err.getMessage))
    }
  }
}