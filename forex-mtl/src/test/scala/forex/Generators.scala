package forex

import forex.domain.{Currency, Price, Rate, Timestamp}
import org.scalacheck.{Arbitrary, Gen}

import java.time.OffsetDateTime

trait Generators {
  implicit def functionArb[A](implicit arbA: Arbitrary[A]): Arbitrary[A => A] = Arbitrary {
    arbA.arbitrary.map(result => (_: A) => result)
  }

  implicit val currencyArb: Arbitrary[Currency] = Arbitrary {
    Gen.oneOf(Currency.all)
  }

  implicit val pairArb: Arbitrary[Rate.Pair] = Arbitrary {
    for {
      from <- currencyArb.arbitrary
      to <- currencyArb.arbitrary
    } yield Rate.Pair(from, to)
  }

  implicit val bigDecimalArb: Arbitrary[BigDecimal] =
    Arbitrary(Gen.choose[BigDecimal](BigDecimal(0), BigDecimal(1)))

  implicit val priceArb: Arbitrary[Price] =
    Arbitrary(Arbitrary.arbitrary[BigDecimal].map(Price.apply))

  implicit val timestampArb: Arbitrary[Timestamp] =
    Arbitrary(Arbitrary.arbitrary[OffsetDateTime].map(Timestamp.apply))

  implicit val rateArb: Arbitrary[Rate] = Arbitrary {
    for {
      pair <- pairArb.arbitrary
      price <- priceArb.arbitrary
      timestamp <- timestampArb.arbitrary
    } yield Rate(pair, price, timestamp)
  }


}
