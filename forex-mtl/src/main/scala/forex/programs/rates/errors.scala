package forex.programs.rates

import forex.services.rates.errors.{ Error => RatesServiceError }
import forex.services.redis.errors.{ Error => RedisServiceError }
object errors {

  sealed trait Error extends Exception
  object Error {
    final case class RateLookupFailed(msg: String) extends Error
    final case class RedisFailed(msg: String) extends Error
  }

  def fromRatesError(error: RatesServiceError): Error = error match {
    case RatesServiceError.OneFrameLookupFailed(msg) => Error.RateLookupFailed(msg)
    case RatesServiceError.OneFrameMalformedResponse(msg) => Error.RateLookupFailed(msg)
  }

  def fromRedisError(error: RedisServiceError): Error = error match {
    case RedisServiceError.RedisLookupError(msg) => Error.RedisFailed(msg)
    case RedisServiceError.RedisMalformedValue(msg) => Error.RedisFailed(msg)
  }
}
