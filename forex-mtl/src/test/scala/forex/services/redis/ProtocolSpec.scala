package forex.services.redis

import forex.BaseSpec
import forex.domain.Rate
import forex.services.redis.Protocol._

class ProtocolSpec extends BaseSpec {
  test("Converting to and from a redis value yields the original value") {
    forAll { (rate: Rate) =>
      assert(fromRedisValue(toRedisValue(rate)) == Right(rate))
    }
  }
}
