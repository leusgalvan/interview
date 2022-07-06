package forex.services.rates.interpreters

import forex.services.rates.Algebra
import cats.Applicative
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.traverse._
import cats.syntax.functor._
import forex.domain.{Price, Rate, Timestamp}
import forex.services.rates.errors._

class OneFrameDummy[F[_]: Applicative] extends Algebra[F] {

  override def get(pairs: List[Rate.Pair]): F[Error Either List[Rate]] = {
    pairs
      .traverse(Rate(_, Price(BigDecimal(100)), Timestamp.now).pure[F])
      .map(_.asRight[Error])
  }

}
