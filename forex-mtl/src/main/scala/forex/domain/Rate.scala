package forex.domain

import cats.Show
import cats.implicits.catsSyntaxTuple2Semigroupal

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

    lazy val all: List[Pair] =
      (Currency.all, Currency.all)
        .mapN { case (c1, c2) => Rate.Pair(c1, c2) }
        .filterNot(p => p.from == p.to)
  }
}
