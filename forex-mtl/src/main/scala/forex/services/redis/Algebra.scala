package forex.services.redis

import forex.domain.Rate
import forex.services.redis.errors.Error

trait Algebra[F[_]] {
  def get(pair: Rate.Pair): F[Error Either Option[Rate]]

  def write(rates: List[Rate]): F[Error Either Unit]

  def delete(pair: Rate.Pair): F[Error Either Long]
}
