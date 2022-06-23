package forex.services.redis.interpreters

import cats.effect.Concurrent
import cats.implicits._
import dev.profunktor.redis4cats.RedisCommands
import forex.domain.Rate
import forex.services.redis.Algebra
import forex.services.redis.errors._

import scala.concurrent.duration._

class RedisService[F[_]: Concurrent](
    commands: RedisCommands[F, String, String],
    expiration: FiniteDuration
) extends Algebra[F] {
  import forex.services.redis.Protocol._

  override def get(pair: Rate.Pair): F[Either[Error, Option[Rate]]] = {
    commands
      .get(toRedisKey(pair))
      .map {
        case Some(value) =>
          fromRedisValue(value).map(Option.apply)
        case None =>
          Right(None)
      }
  }

  override def write(rate: Rate): F[Either[Error, Unit]] = {
    val key = toRedisKey(rate.pair)
    val value = toRedisValue(rate)
    commands.setEx(key, value, expiration).map(Right.apply)
  }

  override def delete(pair: Rate.Pair): F[Either[Error, Long]] = {
    val key = toRedisKey(pair)
    commands.del(key).map(Right.apply)
  }
}

object RedisService {
  def apply[F[_]: Concurrent](
     commands: RedisCommands[F, String, String],
     expiration: FiniteDuration
  ): RedisService[F] =
    new RedisService[F](commands, expiration)
}