package forex.services.redis

import forex.domain.Rate
import forex.services.redis.errors.Error.RedisMalformedValue
import forex.services.redis.errors._
import io.circe.generic.auto._
import io.circe.syntax._

import scala.util.{Failure, Success, Try}

object Protocol {
  def toRedisKey(pair: Rate.Pair): String =
    s"${pair.from}${pair.to}"

  def toRedisValue(rate: Rate): String =
    rate.asJson.noSpaces

  def fromRedisValue(value: String): Either[Error, Rate] = {
    Try(value.asJson).map(_.as[Rate]) match {
      case Success(Right(rate)) => Right(rate)
      case Success(Left(decodeFailure)) => Left(RedisMalformedValue(decodeFailure.message))
      case Failure(exception) => Left(RedisMalformedValue(exception.getMessage))
    }
  }
}