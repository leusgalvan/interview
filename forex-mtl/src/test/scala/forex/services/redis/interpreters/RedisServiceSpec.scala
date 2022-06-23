package forex.services.redis.interpreters

import cats.effect.{ContextShift, IO, Timer}
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.connection.RedisClient
import dev.profunktor.redis4cats.data.RedisCodec
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.domain.Rate.Pair
import org.scalatest.funsuite.AnyFunSuite
import dev.profunktor.redis4cats.effect.Log.NoOp._
import org.typelevel.log4cats._
import org.typelevel.log4cats.slf4j._

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class RedisServiceSpec extends AnyFunSuite {
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)
  implicit val timer: Timer[IO] = IO.timer(ec)
  implicit val logger: Logger[IO] = LoggerFactory[IO].getLogger
  private val redisUri = "redis://localhost:6379"
  private val defaultExpiration = 5.seconds

  def withService[A](expiration: FiniteDuration)(f: RedisService[IO] => IO[A]): A = {
    val commandsRes = RedisClient[IO].from(redisUri).flatMap(Redis[IO].fromClient(_, RedisCodec.Utf8))

    commandsRes.use { commands =>
      val service = RedisService[IO](commands, expiration)
      f(service)
    }.unsafeRunSync()
  }

  // docker pull redis
  // docker run -p 6379:6379 redis
  test("Getting a non-existing value yields None") {
    val currencies = Pair(Currency.USD, Currency.CAD)
    val result = withService(defaultExpiration) { service =>
      service.delete(currencies) *> service.get(currencies)
    }
    assert(result == Right(None))
  }

  test("Writing a value and then reading it yields the value") {
    val currencies = Pair(Currency.USD, Currency.CAD)
    val rate = Rate(currencies, Price.fromInt(10), Timestamp.now)
    val result = withService(defaultExpiration) { service =>
      service.delete(currencies) *> service.write(rate) *> service.get(currencies)
    }
    assert(result == Right(Some(rate)))
  }

  test("Values expire in provided duration") {
    val currencies = Pair(Currency.USD, Currency.CAD)
    val rate = Rate(currencies, Price.fromInt(10), Timestamp.now)
    val result = withService(1.second) { service =>
      service.delete(currencies) *> service.write(rate) *> IO.sleep(1100.millis) *> service.get(currencies)
    }
    assert(result == Right(None))
  }
}
