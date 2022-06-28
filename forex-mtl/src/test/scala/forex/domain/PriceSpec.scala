package forex.domain

import cats.Eq
import cats.kernel.laws.discipline.MonoidTests
import forex.BaseSpec

class PriceSpec extends BaseSpec {
  implicit val eqPrice: Eq[Price] = Eq.instance { case (p1, p2) =>
    (p1.value - p2.value).abs < BigDecimal("0.00000000001")
  }

  checkAll("Monoid[Price]", MonoidTests[Price].monoid)
}
