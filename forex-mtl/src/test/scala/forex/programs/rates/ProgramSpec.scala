package forex.programs.rates

import cats.effect.{ContextShift, IO}
import forex.BaseSpec
import forex.domain.Rate.Pair
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.programs.rates.Protocol.GetRatesRequest
import forex.programs.rates.errors.Error.{RateLookupFailed, RedisFailed}
import forex.services.{RatesService, RedisService, rates, redis}

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Try}

class ProgramSpec extends BaseSpec {
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)

  private val now = Timestamp.now

  test("return immediately when from and to currencies are the same") {
    val rate = Rate(Pair(Currency.USD, Currency.USD), Price.fromInt(1), Timestamp.now)
    val ratesService = dummyRatesService
    val redisService = dummyRedisService
    val program = Program[IO](ratesService, redisService)
    val request = GetRatesRequest(from = Currency.USD, to = Currency.USD)
    val result = program.get(request).unsafeRunSync().map(_.copy(timestamp = rate.timestamp))
    assert(result == Right(rate))
  }

  test("return cached rate when present") {
    val rate = Rate(Rate.Pair(Currency.NZD, Currency.GBP), Price.fromInt(5), now)
    val ratesService: RatesService[IO] = dummyRatesService
    val redisService: RedisService[IO] = constRedisService(Some(rate))
    val program = Program[IO](ratesService, redisService)
    val request = GetRatesRequest(rate.pair.from, rate.pair.to)
    val result = program.get(request).unsafeRunSync()
    assert(result == Right(rate))
  }

  test("return rate from service when NOT present in redis") {
    val rate = Rate(Rate.Pair(Currency.NZD, Currency.GBP), Price.fromInt(5), now)
    val ratesService: RatesService[IO] = constRatesService(List(rate))
    val redisService: RedisService[IO] = constRedisService(Option.empty)
    val program = Program[IO](ratesService, redisService)
    val request = GetRatesRequest(rate.pair.from, rate.pair.to)
    val result = program.get(request).unsafeRunSync()
    assert(result == Right(rate))
  }

  test("convert RedisLookupError given by redis service into RedisFailed") {
    val ratesService: RatesService[IO] = dummyRatesService
    val redisService: RedisService[IO] = failingRedisService(redis.errors.Error.RedisLookupError("redis error"))
    val program = Program[IO](ratesService, redisService)
    val request = GetRatesRequest(from = Currency.USD, to = Currency.CAD)
    val result = program.get(request).unsafeRunSync()
    assert(result == Left(RedisFailed("redis error")))
  }

  test("convert RedisMalformedValue given by redis service into RedisFailed") {
    val ratesService: RatesService[IO] = dummyRatesService
    val redisService: RedisService[IO] = failingRedisService(redis.errors.Error.RedisMalformedValue("redis error"))
    val program = Program[IO](ratesService, redisService)
    val request = GetRatesRequest(from = Currency.USD, to = Currency.CAD)
    val result = program.get(request).unsafeRunSync()
    assert(result == Left(RedisFailed("redis error")))
  }

  test("convert OneFrameLookup error given by rates service into RateLookupFailed") {
    val ratesService: RatesService[IO] = new rates.Algebra[IO] {
      override def get(pairs: List[Pair]): IO[Either[rates.errors.Error, List[Rate]]] =
        IO(Left(rates.errors.Error.OneFrameLookupFailed("lookup failed")))
    }
    val redisService: RedisService[IO] = constRedisService(Option.empty)
    val program = Program[IO](ratesService, redisService)
    val request = GetRatesRequest(from = Currency.USD, to = Currency.CAD)
    val result = program.get(request).unsafeRunSync()
    assert(result == Left(RateLookupFailed("lookup failed")))
  }

  test("raise unhandled errors given by rates service") {
    val error = new Exception("unhandled error")
    val ratesService: RatesService[IO] = new rates.Algebra[IO] {
      override def get(pairs: List[Pair]): IO[Either[rates.errors.Error, List[Rate]]] =
        IO.raiseError(error)
    }
    val redisService: RedisService[IO] = constRedisService(Option.empty)
    val program = Program[IO](ratesService, redisService)
    val request = GetRatesRequest(from = Currency.USD, to = Currency.CAD)
    val result = Try(program.get(request).unsafeRunSync())
    assert(result == Failure(error))
  }
}
