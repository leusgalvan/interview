package forex.services.redis

import forex.BaseSpec
import forex.domain.Rate
import forex.services.redis.Protocol._

class ProtocolSpec extends BaseSpec {
  test("Converting to and from a redis value yields the original value") {
    forAll { (rates: List[Rate]) =>
      assert(fromRedisValue(toRedisValue(rates)) == Right(rates))
    }
  }
}
