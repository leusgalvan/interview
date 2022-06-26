package forex.programs.rates

import cats.effect.IO
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.domain.Rate.Pair
import forex.programs.rates.Protocol.GetRatesRequest
import forex.programs.rates.errors.Error.{RateLookupFailed, RedisFailed}
import forex.services.RatesService
import forex.services.rates
import forex.services.redis
import forex.services.RedisService
import forex.services.redis.errors
import org.scalatest.funsuite.AnyFunSuite

import scala.util.{Failure, Try}

class ProgramSpec extends AnyFunSuite {
  private def failingRedisService(error: errors.Error): RedisService[IO] = new RedisService[IO] {
    override def get(pair: Pair): IO[Either[errors.Error, Option[Rate]]] =
      IO(Left(error))

    override def write(rate: Rate): IO[Either[errors.Error, Unit]] =
      IO(Left(error))

    override def delete(pair: Pair): IO[Either[errors.Error, Long]] =
      IO(Left(error))
  }

  private def mapRedisService(rateMap: Map[Pair, Rate]): RedisService[IO] = new redis.Algebra[IO] {
    override def get(pair: Pair): IO[Either[errors.Error, Option[Rate]]] =
      IO(Right(rateMap.get(pair)))

    override def write(rate: Rate): IO[Either[errors.Error, Unit]] =
      IO(Right(()))

    override def delete(pair: Pair): IO[Either[errors.Error, Long]] =
      IO(Right(0))
  }

  private def constRedisService(rateOpt: Option[Rate]): RedisService[IO] =
    mapRedisService(rateOpt.fold(Map.empty[Pair, Rate])(rate => Map(rate.pair -> rate)))

  test("return cached rate when present in redis") {
    val rate = Rate(Pair(Currency.USD, Currency.CAD), Price.fromInt(10), Timestamp.now)
    val ratesService: RatesService[IO] = new rates.Algebra[IO] {
      override def get(pair: Pair): IO[Either[rates.errors.Error, Rate]] =
        IO.raiseError(new Exception("should not be called"))
    }
    val redisService: RedisService[IO] = constRedisService(Some(rate))
    val program = Program[IO](ratesService, redisService)
    val request = GetRatesRequest(from = Currency.USD, to = Currency.CAD)
    val result = program.get(request).unsafeRunSync()
    assert(result == Right(rate))
  }

  test("return inverse cached rate when inverse rate present in redis") {
    val timestamp = Timestamp.now
    val rate = Rate(Pair(Currency.USD, Currency.CAD), Price.fromInt(10), timestamp)
    val inverseRate = Rate(Pair(Currency.CAD, Currency.USD), Price(0.1), timestamp)
    val ratesService: RatesService[IO] = new rates.Algebra[IO] {
      override def get(pair: Pair): IO[Either[rates.errors.Error, Rate]] =
        IO.raiseError(new Exception("should not be called"))
    }
    val redisService: RedisService[IO] = constRedisService(Some(inverseRate))
    val program = Program[IO](ratesService, redisService)
    val request = GetRatesRequest(from = Currency.USD, to = Currency.CAD)
    val result = program.get(request).unsafeRunSync()
    assert(result == Right(rate))
  }

  test("return rate from service when NOT present in redis") {
    val rate = Rate(Pair(Currency.USD, Currency.CAD), Price.fromInt(10), Timestamp.now)
    val ratesService: RatesService[IO] = new rates.Algebra[IO] {
      override def get(pair: Pair): IO[Either[rates.errors.Error, Rate]] =
        IO.pure(Right(rate))
    }
    val redisService: RedisService[IO] = constRedisService(None)
    val program = Program[IO](ratesService, redisService)
    val request = GetRatesRequest(from = Currency.USD, to = Currency.CAD)
    val result = program.get(request).unsafeRunSync()
    assert(result == Right(rate))
  }

  test("convert RedisLookupError given by redis service into RedisFailed") {
    val ratesService: RatesService[IO] = new rates.Algebra[IO] {
      override def get(pair: Pair): IO[Either[rates.errors.Error, Rate]] =
        IO.raiseError(new Exception("should not be called"))
    }
    val redisService: RedisService[IO] = failingRedisService(redis.errors.Error.RedisLookupError("redis error"))
    val program = Program[IO](ratesService, redisService)
    val request = GetRatesRequest(from = Currency.USD, to = Currency.CAD)
    val result = program.get(request).unsafeRunSync()
    assert(result == Left(RedisFailed("redis error")))
  }

  test("convert RedisMalformedValue given by redis service into RedisFailed") {
    val ratesService: RatesService[IO] = new rates.Algebra[IO] {
      override def get(pair: Pair): IO[Either[rates.errors.Error, Rate]] =
        IO.raiseError(new Exception("should not be called"))
    }
    val redisService: RedisService[IO] = failingRedisService(redis.errors.Error.RedisMalformedValue("redis error"))
    val program = Program[IO](ratesService, redisService)
    val request = GetRatesRequest(from = Currency.USD, to = Currency.CAD)
    val result = program.get(request).unsafeRunSync()
    assert(result == Left(RedisFailed("redis error")))
  }

  test("convert OneFrameLookup error given by rates service into RateLookupFailed") {
    val ratesService: RatesService[IO] = new rates.Algebra[IO] {
      override def get(pair: Pair): IO[Either[rates.errors.Error, Rate]] =
        IO(Left(rates.errors.Error.OneFrameLookupFailed("lookup failed")))
    }
    val redisService: RedisService[IO] = constRedisService(None)
    val program = Program[IO](ratesService, redisService)
    val request = GetRatesRequest(from = Currency.USD, to = Currency.CAD)
    val result = program.get(request).unsafeRunSync()
    assert(result == Left(RateLookupFailed("lookup failed")))
  }

  test("raise unhandled errors given by rates service") {
    val error = new Exception("unhandled error")
    val ratesService: RatesService[IO] = new rates.Algebra[IO] {
      override def get(pair: Pair): IO[Either[rates.errors.Error, Rate]] =
        IO.raiseError(error)
    }
    val redisService: RedisService[IO] = constRedisService(None)
    val program = Program[IO](ratesService, redisService)
    val request = GetRatesRequest(from = Currency.USD, to = Currency.CAD)
    val result = Try(program.get(request).unsafeRunSync())
    assert(result == Failure(error))
  }
}
