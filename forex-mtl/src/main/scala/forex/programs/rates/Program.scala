package forex.programs.rates

import cats.Monad
import cats.data.EitherT
import forex.domain._
import forex.programs.rates.errors._
import forex.services.{RatesService, RedisService}

class Program[F[_]: Monad](
    ratesService: RatesService[F],
    redisService: RedisService[F]
) extends Algebra[F] {

  override def get(request: Protocol.GetRatesRequest): F[Error Either Rate] = {
    val pair = Rate.Pair(request.from, request.to)
    val redisValueOpt = EitherT(redisService.get(pair)).leftMap(fromRedisError)
    lazy val ratesServiceValue = EitherT(ratesService.get(pair)).leftMap(fromRatesError)
    def updateCache(rate: Rate): F[Error Either Unit] =
      EitherT(redisService.write(rate)).leftMap(fromRedisError).value

    redisValueOpt.flatMap { cachedRateOpt =>
      cachedRateOpt.fold(ratesServiceValue.semiflatTap(updateCache))(rate => EitherT.pure[F, Error](rate))
    }.value
  }

}

object Program {

  def apply[F[_]: Monad](
      ratesService: RatesService[F],
      redisService: RedisService[F]
  ): Algebra[F] = new Program[F](ratesService, redisService)

}
