package forex.services.redis

import forex.domain.{Currency, Price, Rate, Timestamp}
import org.scalatest.funsuite.AnyFunSuite
import Protocol._

class ProtocolSpec extends AnyFunSuite {
  private val rates = Currency.all.zip(Currency.all.tail).map { case (from, to) =>
    Rate(Rate.Pair(from, to), Price(BigDecimal(math.random())), Timestamp.now)
  }

  test("Converting to and from a redis value yields the original value") {
    val result = fromRedisValue(toRedisValue(rates))
    assert(result == Right(rates))
  }
}
