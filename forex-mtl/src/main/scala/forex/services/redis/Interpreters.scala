package forex.services.redis

import cats.effect.{Concurrent, ContextShift}
import dev.profunktor.redis4cats.connection.RedisClient
import forex.services.redis.interpreters.RedisService

import scala.concurrent.duration.FiniteDuration

object Interpreters {
  def live[F[_]: ContextShift: Concurrent](redisClient: RedisClient, expiration: FiniteDuration): RedisService[F] =
    RedisService[F](redisClient, expiration)
}
