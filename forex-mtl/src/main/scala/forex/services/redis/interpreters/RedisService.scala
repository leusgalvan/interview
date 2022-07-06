package forex.services.redis.interpreters

import cats.data.EitherT
import cats.implicits._
import cats.{MonadError, Parallel}
import dev.profunktor.redis4cats._
import forex.domain.Rate
import forex.domain.Rate.Pair
import forex.services.redis.Algebra
import forex.services.redis.errors._

import scala.concurrent.duration._

class RedisService[F[_]: Parallel](
    commands: RedisCommands[F, String, String],
    expiration: FiniteDuration
)(implicit AE: MonadError[F, Throwable]) extends Algebra[F] {
  import forex.services.redis.Protocol._

  override def get(pair: Pair): F[Either[Error, Option[Rate]]] = {
    commands
      .get(toRedisKey(pair))
      .attemptT
      .leftMap[Error](e => Error.RedisLookupError(e.getMessage))
      .flatMap(x => EitherT.fromEither(x.traverse(fromRedisValue)))
      .value
  }

  override def write(rates: List[Rate]): F[Either[Error, Unit]] = {
    rates
      .parTraverse_(r => commands.setEx(toRedisKey(r.pair), toRedisValue(r), expiration))
      .attemptT
      .leftMap[Error](e => Error.RedisWriteError(e.getMessage))
      .value
  }

  override def delete(pair: Pair): F[Either[Error, Long]] = {
    commands
      .del(toRedisKey(pair))
      .attemptT
      .leftMap[Error](e => Error.RedisDeleteError(e.getMessage))
      .value
  }
}

object RedisService {
  def apply[F[_]: Parallel](
     commands: RedisCommands[F, String, String],
     expiration: FiniteDuration
  )(implicit AE: MonadError[F, Throwable]): RedisService[F] =
    new RedisService[F](commands, expiration)
}