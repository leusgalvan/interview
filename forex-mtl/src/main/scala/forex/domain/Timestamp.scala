package forex.domain

import cats.Order
import org.typelevel.cats.time._
import java.time.OffsetDateTime

case class Timestamp(value: OffsetDateTime) extends AnyVal

object Timestamp {
  def now: Timestamp =
    Timestamp(OffsetDateTime.now)

  implicit val order: Order[Timestamp] = Order.by(_.value)
}
