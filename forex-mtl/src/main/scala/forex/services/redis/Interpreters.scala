package forex.services.redis

import cats.{MonadError, Parallel}
import dev.profunktor.redis4cats.RedisCommands
import forex.services.redis.interpreters.RedisService

import scala.concurrent.duration.FiniteDuration

object Interpreters {
  def live[F[_]: Parallel](
      commands: RedisCommands[F, String, String],
      expiration: FiniteDuration
  )(implicit AE: MonadError[F, Throwable]): RedisService[F] =
    RedisService[F](commands, expiration)
}
