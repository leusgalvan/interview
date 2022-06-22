package forex.services.redis.interpreters

import cats.effect.{ContextShift, IO, Timer}
import dev.profunktor.redis4cats.connection.RedisClient
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.domain.Rate.Pair
import org.scalatest.funsuite.AnyFunSuite
import dev.profunktor.redis4cats.effect.Log.NoOp._

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class RedisServiceSpec extends AnyFunSuite {
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)
  implicit val timer: Timer[IO] = IO.timer(ec)

  private val redisUri = "redis://localhost:6379"
  private val defaultExpiration = 5.seconds

  // docker pull redis
  // docker run -p 6379:6379 redis
  test("Getting a non-existing value yields None") {
    val result = RedisClient[IO].from(redisUri).use { client =>
      val service = RedisService[IO](client, defaultExpiration)
      val currencies = Pair(Currency.USD, Currency.CAD)
      service.delete(currencies) *> service.get(currencies)
    }.unsafeRunSync()

    assert(result == Right(None))
  }

  test("Writing a value and then reading it yields the value") {
    val currencies = Pair(Currency.USD, Currency.CAD)
    val rate = Rate(currencies, Price.fromInt(10), Timestamp.now)
    val result = RedisClient[IO].from(redisUri).use { client =>
      val service = RedisService[IO](client, defaultExpiration)
      service.delete(currencies) *> service.write(rate) *> service.get(currencies)
    }.unsafeRunSync()

    assert(result == Right(Some(rate)))
  }

  test("Values expire in provided duration") {
    val currencies = Pair(Currency.USD, Currency.CAD)
    val rate = Rate(currencies, Price.fromInt(10), Timestamp.now)
    val result = RedisClient[IO].from(redisUri).use { client =>
      val service = RedisService[IO](client, 1.second)
      service.delete(currencies) *> service.write(rate) *> IO.sleep(1100.millis) *> service.get(currencies)
    }.unsafeRunSync()

    assert(result == Right(None))
  }
}
