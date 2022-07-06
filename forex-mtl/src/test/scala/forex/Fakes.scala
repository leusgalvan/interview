package forex

import cats.effect.IO
import cats.implicits._
import forex.domain.Rate
import forex.domain.Rate.Pair
import forex.services.{RatesService, RedisService, rates, redis}
import forex.services.redis.errors

trait Fakes {
  protected def failingRedisService(error: errors.Error): RedisService[IO] = new RedisService[IO] {
    override def get(pair: Pair): IO[Either[errors.Error, Option[Rate]]] =
      IO(Left(error))

    override def write(rates: List[Rate]): IO[Either[errors.Error, Unit]] =
      IO(Left(error))

    override def delete(pair: Pair): IO[Either[errors.Error, Long]] =
      IO(Left(error))
  }

  protected def constRedisService(rateOpt: Option[Rate]): RedisService[IO] = new RedisService[IO] {
    override def get(pair: Pair): IO[Either[errors.Error, Option[Rate]]] =
      IO(Right(rateOpt))

    override def write(rates: List[Rate]): IO[Either[errors.Error, Unit]] =
      IO(Right(()))

    override def delete(pair: Pair): IO[Either[errors.Error, Long]] =
      IO(Right(0))
  }

  def constRatesService(rate: List[Rate]): RatesService[IO] = new RatesService[IO] {
    override def get(pairs: List[Pair]): IO[Either[rates.errors.Error, List[Rate]]] =
      IO(rate.asRight[rates.errors.Error])
  }

  val dummyRatesService: RatesService[IO] = new rates.Algebra[IO] {
    override def get(pairs: List[Pair]): IO[Either[rates.errors.Error, List[Rate]]] =
      IO.raiseError(new Exception("should not be called"))
  }

  val dummyRedisService: RedisService[IO] = new redis.Algebra[IO] {
    override def get(pair: Pair): IO[Either[errors.Error, Option[Rate]]] =
      IO.raiseError(new Exception("should not be called"))

    override def write(rates: List[Rate]): IO[Either[errors.Error, Unit]] =
      IO.raiseError(new Exception("should not be called"))

    override def delete(pair: Pair): IO[Either[errors.Error, Long]] =
      IO.raiseError(new Exception("should not be called"))
  }
}
