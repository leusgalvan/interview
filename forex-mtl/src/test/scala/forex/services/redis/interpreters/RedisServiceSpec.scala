package forex.services.redis.interpreters

import cats.effect.{ContextShift, IO, Timer}
import cats.implicits.catsSyntaxTuple2Semigroupal
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.connection.RedisClient
import dev.profunktor.redis4cats.data.RedisCodec
import forex.domain.{Currency, Price, Rate, Timestamp}
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
  private val now = Timestamp.now

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
    val pair = Rate.Pair(Currency.CAD, Currency.USD)
    val result = withService(defaultExpiration) { service =>
      service.delete(pair) *> service.get(pair)
    }
    assert(result == Right(None))
  }

  test("Writing two values and then reading them yields the original values") {
    val rate1 = Rate(Rate.Pair(Currency.CAD, Currency.CHF), Price.fromInt(2), now)
    val rate2 = Rate(Rate.Pair(Currency.JPY, Currency.USD), Price.fromInt(3), now)
    val result = withService(defaultExpiration) { service =>
      service.delete(rate1.pair) *> service.delete(rate2.pair) *>
        service.write(List(rate1, rate2)) *>
        (service.get(rate1.pair), service.get(rate2.pair)).tupled
    }
    assert(result == (Right(Some(rate1)) -> Right(Some(rate2))))
  }

  test("Values expire in provided duration") {
    val rate1 = Rate(Rate.Pair(Currency.CAD, Currency.CHF), Price.fromInt(2), now)
    val rate2 = Rate(Rate.Pair(Currency.JPY, Currency.USD), Price.fromInt(3), now)
    val result = withService(1.second) { service =>
      service.delete(rate1.pair) *> service.delete(rate2.pair) *>
        service.write(List(rate1, rate2)) *>
        IO.sleep(1100.millis) *>
        (service.get(rate1.pair), service.get(rate2.pair)).tupled
    }
    assert(result == (Right(None) -> Right(None)))
  }
}
