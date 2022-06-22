package forex.services.rates

import cats.Applicative
import cats.effect.Sync
import interpreters._
import org.http4s.client.Client

object Interpreters {
  def dummy[F[_]: Applicative]: Algebra[F] = new OneFrameDummy[F]()
  def live[F[_]: Sync](client: Client[F], oneFrameHost: String, oneFramePort: Int): Algebra[F] =
    OneFrameService[F](client, oneFrameHost, oneFramePort)
}
