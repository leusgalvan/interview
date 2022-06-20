package forex.services.redis.interpreters

import cats.effect.{ContextShift, IO}
import dev.profunktor.redis4cats.connection.RedisClient
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.domain.Rate.Pair
import org.scalatest.funsuite.AnyFunSuite
import dev.profunktor.redis4cats.effect.Log.NoOp._
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

class RedisServiceSpec extends AnyFunSuite {
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)
  val redisUri = "redis://localhost:6379"

  // docker pull redis
  // docker run -p 6379:6379 redis
  test("Getting a non-existing value yields None") {
    val result = RedisClient[IO].from(redisUri).use { client =>
      val service = RedisService[IO](client)
      val currencies = Pair(Currency.USD, Currency.CAD)
      service.delete(currencies) *> service.get(currencies)
    }.unsafeRunSync()

    assert(result == Right(None))
  }

  test("Writing a value and then reading it yields the value") {
    val currencies = Pair(Currency.USD, Currency.CAD)
    val rate = Rate(currencies, Price.fromInt(10), Timestamp.now)
    val result = RedisClient[IO].from(redisUri).use { client =>
      val service = RedisService[IO](client)
      service.delete(currencies) *> service.write(rate) *> service.get(currencies)
    }.unsafeRunSync()

    assert(result == Right(Some(rate)))
  }
}
