package forex.programs.rates

import cats.Monad
import cats.data.EitherT
import forex.domain._
import forex.programs.rates.errors._
import forex.services.{RatesService, RedisService}
import cats.syntax.flatMap._
import cats.syntax.applicative._
import cats.syntax.functor._
import cats.syntax.either._

class Program[F[_]: Monad](
    ratesService: RatesService[F],
    redisService: RedisService[F]
) extends Algebra[F] {

  override def get(request: Protocol.GetRatesRequest): F[Error Either Rate] = {
    def fromRedis(pair: Rate.Pair): F[Either[Error, Option[Rate]]] = {
      redisService.get(pair).flatMap {
        case Left(error) =>
          fromRedisError(error).asLeft[Option[Rate]].pure[F]

        case Right(None) =>
          redisService.get(pair.inverted).map { redisResult =>
            redisResult.map { rateOpt =>
              rateOpt.map { rate =>
                rate.inverted
              }
            }.leftMap(fromRedisError)
          }

        case Right(r) =>
          r.asRight[Error].pure[F]
      }
    }

    val pair = Rate.Pair(request.from, request.to)
    lazy val ratesServiceValue = EitherT(ratesService.get(pair)).leftMap(fromRatesError)
    def updateCache(rate: Rate): F[Error Either Unit] =
      EitherT(redisService.write(rate)).leftMap(fromRedisError).value

    EitherT(fromRedis(pair)).flatMap { cachedRateOpt =>
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
