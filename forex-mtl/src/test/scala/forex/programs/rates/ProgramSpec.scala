package forex.programs.rates

import cats.effect.IO
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.domain.Rate.Pair
import forex.programs.rates.Protocol.GetRatesRequest
import forex.programs.rates.errors.Error.RateLookupFailed
import forex.services.RatesService
import forex.services.rates
import forex.services.redis
import forex.services.RedisService
import forex.services.redis.errors
import org.scalatest.funsuite.AnyFunSuite

import scala.util.{Failure, Try}

class ProgramSpec extends AnyFunSuite {
  private def constRedisService(rateOpt: Option[Rate]): RedisService[IO] = new redis.Algebra[IO] {
    override def get(pair: Pair): IO[Either[errors.Error, Option[Rate]]] =
      IO(Right(rateOpt))

    override def write(rate: Rate): IO[Either[errors.Error, Unit]] =
      IO(Right(()))

    override def delete(pair: Pair): IO[Either[errors.Error, Long]] =
      IO(Right(0))
  }

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
