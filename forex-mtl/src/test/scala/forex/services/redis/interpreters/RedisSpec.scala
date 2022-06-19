package forex.services.redis.interpreters

import cats.effect.{ContextShift, IO}
import dev.profunktor.redis4cats.connection.RedisClient
import forex.domain.Currency
import forex.domain.Rate.Pair
import org.scalatest.funsuite.AnyFunSuite
import dev.profunktor.redis4cats.effect.Log.NoOp._
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

class RedisSpec extends AnyFunSuite {
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)

  // docker pull redis
  // docker run -p 6379:6379 redis
  test("Getting a non-existing value yields None") {
    val result = RedisClient[IO].from("redis://localhost:6379").use { client =>
      val service = RedisService[IO](client)
      val currencies = Pair(Currency.USD, Currency.CAD)
      service.get(currencies)
    }.unsafeRunSync()

    assert(result == Right(None))
  }
}
