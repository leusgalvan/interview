package forex.domain

import cats.Show

case class Rate(
    pair: Rate.Pair,
    price: Price,
    timestamp: Timestamp
)

object Rate {
  final case class Pair(
      from: Currency,
      to: Currency
  )

  object Pair {
    implicit val show: Show[Pair] = Show.show(p => s"${p.from}${p.to}")
  }
}
