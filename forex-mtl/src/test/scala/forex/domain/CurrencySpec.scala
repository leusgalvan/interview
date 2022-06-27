package forex.domain

import org.scalatest.funsuite.AnyFunSuite

class CurrencySpec extends AnyFunSuite {
  import Currency._

  test("Path of length 0") {
    assert(findPath(USD, USD).isEmpty)
  }

  test("Path of length 1") {
    assert(findPath(JPY, NZD) == List(Rate.Pair(JPY, NZD)))
  }

  test("Path of length 2") {
    assert(findPath(CHF, GBP) == List(Rate.Pair(CHF, EUR), Rate.Pair(EUR, GBP)))
  }

  test("Path of max length") {
    assert(findPath(AUD, USD) == List(
      Rate.Pair(AUD, CAD),
      Rate.Pair(CAD, CHF),
      Rate.Pair(CHF, EUR),
      Rate.Pair(EUR, GBP),
      Rate.Pair(GBP, JPY),
      Rate.Pair(JPY, NZD),
      Rate.Pair(NZD, SGD),
      Rate.Pair(SGD, USD)
    ))
  }

  test("Reverse path is empty") {
    assert(findPath(JPY, CAD).isEmpty)
  }
}
