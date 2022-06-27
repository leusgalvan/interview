package forex.services.redis.interpreters

import cats.Functor
import cats.implicits._
import dev.profunktor.redis4cats.RedisCommands
import forex.domain.Rate
import forex.services.redis.Algebra
import forex.services.redis.errors._

import scala.concurrent.duration._

class RedisService[F[_]: Functor](
    commands: RedisCommands[F, String, String],
    expiration: FiniteDuration
) extends Algebra[F] {
  import forex.services.redis.Protocol._

  override def get: F[Either[Error, List[Rate]]] = {
    commands.get(ratesKey)
      .map {
        case Some(rates) =>
          fromRedisValue(rates)
        case None =>
          Right(Nil)
      }
  }

  override def write(rates: List[Rate]): F[Either[Error, Unit]] = {
    commands.setEx(ratesKey, toRedisValue(rates), expiration).map(Right.apply)
  }

  override def delete: F[Either[Error, Long]] = {
    commands.del(ratesKey).map(Right.apply)
  }
}

object RedisService {
  def apply[F[_]: Functor](
     commands: RedisCommands[F, String, String],
     expiration: FiniteDuration
  ): RedisService[F] =
    new RedisService[F](commands, expiration)
}