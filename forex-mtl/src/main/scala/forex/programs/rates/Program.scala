package forex.programs.rates

import cats.data.EitherT
import cats.implicits._
import cats.{Monad, Order, Parallel}
import forex.domain.Rate.Pair
import forex.domain._
import forex.programs.rates.errors._
import forex.services.{RatesService, RedisService}

class Program[F[_]: Monad: Parallel](
    ratesService: RatesService[F],
    redisService: RedisService[F]
) extends Algebra[F] {
  override def get(request: Protocol.GetRatesRequest): F[Error Either Rate] = {
    if(Order.lt(request.from, request.to)) {
      getRate(Rate.Pair(request.from, request.to))
    } else if (Order.gt(request.from, request.to)) {
      EitherT(get(request.inverted)).map(_.inverted).value
    } else {
      Rate(Rate.Pair(request.from, request.to), Price.fromInt(1), Timestamp.now)
        .asRight[Error]
        .pure[F]
    }
  }

  // We can work under the assumption that from < to
  private def getRate(pair: Rate.Pair): F[Error Either Rate] = {
    fetchRedisRates.flatMap { cachedRates =>
      if(cachedRates.nonEmpty) {
        calculateRate(pair, cachedRates)
      } else {
        fetchOneFrameRates.flatMap { rates =>
          updateCache(rates) *> calculateRate(pair, rates)
        }
      }
    }.value
  }

  private def fetchRedisRates = {
    EitherT(redisService.get).leftMap(fromRedisError)
  }

  private def updateCache(rates: List[Rate]) = {
    EitherT(redisService.write(rates)).leftMap(fromRedisError)
  }

  private def fetchOneFrameRates = {
    EitherT(
      Currency.all.zip(Currency.all.tail).map { case (c1, c2) => Rate.Pair(c1, c2) }
        .parTraverse(ratesService.get)
        .map(_.sequence)
    ).leftMap(fromRatesError)
  }

  private def calculateRate(pair: Pair, cachedRates: List[Rate]) = {
    val ratesMap = cachedRates.groupBy(_.pair).view.mapValues(_.head)
    EitherT(
      Currency
        .findPath(pair.from, pair.to)
        .traverse(ratesMap.get)
        .map { rates =>
          val price = rates.map(_.price).combineAll
          val timestamp = rates.map(_.timestamp).minOption.getOrElse(Timestamp.now)
          Rate(pair, price, timestamp)
        }
        .toRight[Error](Error.MissingCachedRate(pair.from.show, pair.to.show))
        .pure[F]
    )
  }
}

object Program {

  def apply[F[_]: Monad: Parallel](
      ratesService: RatesService[F],
      redisService: RedisService[F]
  ): Algebra[F] = new Program[F](ratesService, redisService)

}
