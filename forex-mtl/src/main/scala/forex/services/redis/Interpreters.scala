package forex.services.redis

import cats.effect.{Concurrent, ContextShift}
import dev.profunktor.redis4cats.connection.RedisClient
import forex.services.redis.interpreters.RedisService

object Interpreters {
  def live[F[_]: ContextShift: Concurrent](redisClient: RedisClient): RedisService[F] =
    RedisService[F](redisClient)
}
