package forex.services.redis

import forex.domain.Rate
import forex.services.redis.errors.Error

trait Algebra[F[_]] {
  def get: F[Error Either List[Rate]]

  def write(rates: List[Rate]): F[Error Either Unit]

  def delete: F[Error Either Long]
}
