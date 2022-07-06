package forex.programs.rates

import cats.data.EitherT
import cats.implicits._
import cats.{Monad}
import forex.domain.Rate.Pair
import forex.domain._
import forex.programs.rates.errors._
import forex.services.{RatesService, RedisService}

class Program[F[_]: Monad](
    ratesService: RatesService[F],
    redisService: RedisService[F]
) extends Algebra[F] {
  override def get(request: Protocol.GetRatesRequest): F[Error Either Rate] = {
    val pair = Pair(request.from, request.to)

    if(pair.from == pair.to) {
      Rate(Rate.Pair(request.from, request.to), Price.fromInt(1), Timestamp.now)
        .asRight[Error]
        .pure[F]
    } else {
      getRate(Rate.Pair(request.from, request.to))
    }
  }

  private def getRate(pair: Rate.Pair): F[Error Either Rate] = {
    fetchRedisRate(pair).flatMap { cachedRateOpt =>
      cachedRateOpt.fold {
        fetchOneFrameRates().flatMap { oneFrameRates =>
          updateCache(oneFrameRates) *> findRateForPair(pair, oneFrameRates)
        }
      }(cachedRate => EitherT(cachedRate.asRight[Error].pure[F]))
    }.value
  }

  private def fetchRedisRate(pair: Pair) = {
    EitherT(redisService.get(pair)).leftMap(fromRedisError)
  }

  private def updateCache(rates: List[Rate]) = {
    EitherT(redisService.write(rates)).leftMap(fromRedisError)
  }

  private def fetchOneFrameRates() = {
    EitherT(ratesService.get(Pair.all)).leftMap(fromRatesError)
  }

  private def findRateForPair(pair: Pair, cachedRates: List[Rate]) = {
    EitherT {
      cachedRates.find(_.pair == pair).toRight[Error](Error.MissingCachedRate(pair.from.show, pair.to.show)).pure[F]
    }
  }
}

object Program {

  def apply[F[_]: Monad](
      ratesService: RatesService[F],
      redisService: RedisService[F]
  ): Algebra[F] = new Program[F](ratesService, redisService)

}
