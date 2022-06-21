package forex.services.redis.interpreters

import cats.effect.{Concurrent, ContextShift}
import cats.implicits._
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.connection.RedisClient
import dev.profunktor.redis4cats.data.RedisCodec
import dev.profunktor.redis4cats.effect.Log.NoOp._
import forex.domain.Rate
import forex.services.redis.Algebra
import forex.services.redis.errors._
import scala.concurrent.duration._

class RedisService[F[_]](client: RedisClient, expirationInSeconds: Int)(implicit
    cs: ContextShift[F],
    concurrent: Concurrent[F]
) extends Algebra[F] {
  import forex.services.redis.Protocol._

  override def get(pair: Rate.Pair): F[Either[Error, Option[Rate]]] = {
    Redis[F].fromClient(client, RedisCodec.Utf8).use { commands =>
      commands
        .get(toRedisKey(pair))
        .map {
          case Some(value) =>
            fromRedisValue(value).map(Option.apply)
          case None =>
            Right(None)
        }
    }
  }

  override def write(rate: Rate): F[Either[Error, Unit]] = {
    Redis[F].fromClient(client, RedisCodec.Utf8).use { commands =>
      val key = toRedisKey(rate.pair)
      val value = toRedisValue(rate)
      commands.setEx(key, value, expirationInSeconds.seconds).map(Right.apply)
    }
  }

  override def delete(pair: Rate.Pair): F[Either[Error, Long]] = {
    Redis[F].fromClient(client, RedisCodec.Utf8).use { commands =>
      val key = toRedisKey(pair)
      commands.del(key).map(Right.apply)
    }
  }
}

object RedisService {
  def apply[F[_]](client: RedisClient, expirationInSeconds: Int)(implicit
      cs: ContextShift[F],
      concurrent: Concurrent[F]
  ): RedisService[F] = new RedisService[F](client, expirationInSeconds)
}