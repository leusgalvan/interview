package forex.services.redis

object errors {
  sealed trait Error

  object Error {
    case class RedisLookupError(msg: String) extends Error
    case class RedisMalformedValue(msg: String) extends Error
    case class RedisDeleteError(msg: String) extends Error
    case class RedisWriteError(msg: String) extends Error
  }
}
