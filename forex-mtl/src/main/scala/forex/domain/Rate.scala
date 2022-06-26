package forex.domain

import cats.Show

case class Rate(
    pair: Rate.Pair,
    price: Price,
    timestamp: Timestamp
) {
  def inverted: Rate = Rate(pair.inverted, price.inverted, timestamp)
}

object Rate {
  final case class Pair(
      from: Currency,
      to: Currency
  ) {
    def inverted: Pair = Pair(to, from)
  }

  object Pair {
    implicit val show: Show[Pair] = Show.show(p => s"${p.from}${p.to}")
  }
}
