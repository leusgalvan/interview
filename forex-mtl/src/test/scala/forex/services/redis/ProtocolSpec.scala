package forex.services.redis

import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.domain.Rate.Pair
import org.scalatest.funsuite.AnyFunSuite
import Protocol._

class ProtocolSpec extends AnyFunSuite {
  test("Keys are the concatenation of the currencies") {
    val currencies = Pair(Currency.AUD, Currency.EUR)
    val key = toRedisKey(currencies)
    assert(key == "AUDEUR")
  }

  test("Converting to and from a redis value yields the original value") {
    val rate = Rate(Pair(Currency.GBP, Currency.JPY), Price.fromInt(10), Timestamp.now)
    val result = fromRedisValue(toRedisValue(rate))
    assert(result == Right(rate))
  }
}
