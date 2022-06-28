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

  private val allRates = Currency.all.zip(Currency.all.tail).map { case (from, to) =>
    Rate(Rate.Pair(from, to), Price(BigDecimal(math.random())), now)
  }

  test("return immediately when from and to currencies are the same") {
    val rate = Rate(Pair(Currency.USD, Currency.USD), Price.fromInt(1), Timestamp.now)
    val ratesService = dummyRatesService
    val redisService = dummyRedisService
    val program = Program[IO](ratesService, redisService)
    val request = GetRatesRequest(from = Currency.USD, to = Currency.USD)
    val result = program.get(request).unsafeRunSync().map(_.copy(timestamp = rate.timestamp))
    assert(result == Right(rate))
  }

  test("compute rate based on cached rates when present") {
    val pair = Rate.Pair(Currency.GBP, Currency.NZD) // idx 4 and 6
    val ratesService: RatesService[IO] = dummyRatesService
    val redisService: RedisService[IO] = constRedisService(allRates)
    val program = Program[IO](ratesService, redisService)
    val request = GetRatesRequest(pair.from, pair.to)
    val result = program.get(request).unsafeRunSync()
    val expectedRate = Rate(
      pair = pair,
      timestamp = now,
      price = allRates(4).price * allRates(5).price
    )
    assert(result == Right(expectedRate))
  }

  test("compute rate based on cached rates when present (inverse case)") {
    val pair = Rate.Pair(Currency.NZD, Currency.GBP) // idx 6 and 4
    val ratesService: RatesService[IO] = dummyRatesService
    val redisService: RedisService[IO] = constRedisService(allRates)
    val program = Program[IO](ratesService, redisService)
    val request = GetRatesRequest(pair.from, pair.to)
    val result = program.get(request).unsafeRunSync()
    val expectedRate = Rate(
      pair = pair,
      timestamp = now,
      price = (allRates(4).price * allRates(5).price).inverted
    )
    assert(result == Right(expectedRate))
  }

  test("return rate from service when NOT present in redis") {
    val pair = Rate.Pair(Currency.GBP, Currency.NZD) // idx 4 and 6
    val ratesService: RatesService[IO] = constRatesService(allRates)
    val redisService: RedisService[IO] = constRedisService(List.empty)
    val program = Program[IO](ratesService, redisService)
    val request = GetRatesRequest(pair.from, pair.to)
    val result = program.get(request).unsafeRunSync()
    val expectedRate = Rate(
      pair = pair,
      timestamp = now,
      price = allRates(4).price * allRates(5).price
    )
    assert(result == Right(expectedRate))
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
      override def get(pair: Pair): IO[Either[rates.errors.Error, Rate]] =
        IO(Left(rates.errors.Error.OneFrameLookupFailed("lookup failed")))
    }
    val redisService: RedisService[IO] = constRedisService(List.empty)
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
    val redisService: RedisService[IO] = constRedisService(List.empty)
    val program = Program[IO](ratesService, redisService)
    val request = GetRatesRequest(from = Currency.USD, to = Currency.CAD)
    val result = Try(program.get(request).unsafeRunSync())
    assert(result == Failure(error))
  }
}
