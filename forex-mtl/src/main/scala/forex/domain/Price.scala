package forex.domain

import scala.util.Try

case class Price(value: BigDecimal) extends AnyVal {
  def inverted: Price = Try(Price(BigDecimal(1) / value)).getOrElse(this)
}

object Price {
  def fromInt(value: Integer): Price =
    Price(BigDecimal(value))
}
