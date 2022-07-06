package forex.domain

case class Price(value: BigDecimal) extends AnyVal

object Price {
  def fromInt(value: Integer): Price =
    Price(BigDecimal(value))
}
