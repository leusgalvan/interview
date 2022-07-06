package forex.domain

import forex.BaseSpec

class RateSpec extends BaseSpec {
  test("All pairs has correct length") {
    val noCurrencies = Currency.all.length
    val noPairs = Rate.Pair.all.length
    assert(noPairs == noCurrencies * (noCurrencies - 1))
  }

  test("All pairs do not contain from equal to to") {
    assert(Rate.Pair.all.forall(p => p.from != p.to))
  }
}
