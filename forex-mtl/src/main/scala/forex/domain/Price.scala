package forex.domain

import cats.Monoid

import scala.util.Try

case class Price(value: BigDecimal) extends AnyVal {
  def inverted: Price = Try(Price(BigDecimal(1) / value)).getOrElse(this)
  def *(other: Price): Price = Price(this.value * other.value)
}

object Price {
  def fromInt(value: Integer): Price =
    Price(BigDecimal(value))

  implicit val prodMonoid: Monoid[Price] = Monoid.instance(
    emptyValue = Price.fromInt(1),
    cmb = (p1, p2) => p1 * p2
  )
}
