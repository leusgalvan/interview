package forex.domain

import cats.kernel.laws.discipline.OrderTests
import forex.BaseSpec

class TimestampSpec extends BaseSpec {
  checkAll("Order[Timestamp]", OrderTests[Timestamp].order)
}
