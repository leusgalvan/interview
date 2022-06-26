package forex.programs.rates

import cats.Monad
import cats.data.EitherT
import cats.implicits.catsSyntaxOptionId
import forex.domain._
import forex.programs.rates.errors._
import forex.services.{RatesService, RedisService}
import forex.services.redis.errors.{Error => RedisError}
class Program[F[_]: Monad](
    ratesService: RatesService[F],
    redisService: RedisService[F]
) extends Algebra[F] {

  override def get(request: Protocol.GetRatesRequest): F[Error Either Rate] = {
    def fromRedis(pair: Rate.Pair): EitherT[F, Error, Option[Rate]] = {
      EitherT(redisService.get(pair)).flatMap {
        case Some(rate) => EitherT.pure[F, RedisError](rate.some)
        case None       => EitherT(redisService.get(pair.inverted)).map(_.map(_.inverted))
      }.leftMap(fromRedisError)
    }

    def fromService(pair: Rate.Pair): EitherT[F, Error, Rate] =
      EitherT(ratesService.get(pair)).leftMap(fromRatesError)

    def updateCache(rate: Rate): F[Error Either Unit] =
      EitherT(redisService.write(rate)).leftMap(fromRedisError).value

    val pair = Rate.Pair(request.from, request.to)

    if(pair.from == pair.to) {
      Monad[F].pure(Right(Rate(pair, Price.fromInt(1), Timestamp.now)))
    } else {
      fromRedis(pair).flatMap {
        case Some(cachedRate) => EitherT.pure[F, Error](cachedRate)
        case None => fromService(pair).semiflatTap(updateCache)
      }.value
    }
  }

}

object Program {

  def apply[F[_]: Monad](
      ratesService: RatesService[F],
      redisService: RedisService[F]
  ): Algebra[F] = new Program[F](ratesService, redisService)

}
