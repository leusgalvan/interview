package forex.domain

import cats.kernel.Order
import cats.kernel.laws.discipline.OrderTests
import forex.BaseSpec

class CurrencySpec extends BaseSpec {
  import Currency._

  test("Path of length 0") {
    forAll { (currency: Currency) =>
      assert(findPath(currency, currency).isEmpty)
    }
  }

  test("Path of length 1") {
    assert(findPath(JPY, NZD) == List(Rate.Pair(JPY, NZD)))
  }

  test("Path of length 2") {
    assert(findPath(CHF, GBP) == List(Rate.Pair(CHF, EUR), Rate.Pair(EUR, GBP)))
  }

  test("Path of max length") {
    assert(
      findPath(AUD, USD) == List(
        Rate.Pair(AUD, CAD),
        Rate.Pair(CAD, CHF),
        Rate.Pair(CHF, EUR),
        Rate.Pair(EUR, GBP),
        Rate.Pair(GBP, JPY),
        Rate.Pair(JPY, NZD),
        Rate.Pair(NZD, SGD),
        Rate.Pair(SGD, USD)
      )
    )
  }

  test("Reverse path is empty") {
    forAll { (c1: Currency, c2: Currency) =>
      assert(findPath(Order.max(c1, c2), Order.min(c1, c2)).isEmpty)
    }
  }

  checkAll("Order[Currency]", OrderTests[Currency].order)
}
