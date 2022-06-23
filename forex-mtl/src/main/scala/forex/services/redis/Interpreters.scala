package forex.services.redis

import cats.effect.Concurrent
import dev.profunktor.redis4cats.RedisCommands
import forex.services.redis.interpreters.RedisService

import scala.concurrent.duration.FiniteDuration

object Interpreters {
  def live[F[_]: Concurrent](
      commands: RedisCommands[F, String, String],
      expiration: FiniteDuration
  ): RedisService[F] =
    RedisService[F](commands, expiration)
}
