package forex.services.redis.interpreters

import cats.effect.{ContextShift, IO, Timer}
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

  def withService[A](expiration: FiniteDuration)(f: RedisService[IO] => IO[A]): A = {
    val commandsRes = RedisClient[IO].from(redisUri).flatMap(Redis[IO].fromClient(_, RedisCodec.Utf8))

    commandsRes.use { commands =>
      val service = RedisService[IO](commands, expiration)
      f(service)
    }.unsafeRunSync()
  }

  private val rates = Currency.all.zip(Currency.all.tail).map { case (from, to) =>
    Rate(Rate.Pair(from, to), Price(BigDecimal(math.random())), Timestamp.now)
  }

  // docker pull redis
  // docker run -p 6379:6379 redis
  test("Getting a non-existing value yields None") {
    val result = withService(defaultExpiration) { service =>
      service.delete *> service.get
    }
    assert(result == Right(List.empty))
  }

  test("Writing a value and then reading it yields the value") {
    val result = withService(defaultExpiration) { service =>
      service.delete *> service.write(rates) *> service.get
    }
    assert(result == Right(rates))
  }

  test("Values expire in provided duration") {
    val result = withService(1.second) { service =>
      service.delete *> service.write(rates) *> IO.sleep(1100.millis) *> service.get
    }
    assert(result == Right(List.empty))
  }
}
