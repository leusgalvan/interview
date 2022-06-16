package forex.http
package rates

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.applicativeError._
import forex.programs.RatesProgram
import forex.programs.rates.errors.Error.RateLookupFailed
import forex.programs.rates.{Protocol => RatesProgramProtocol}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

import scala.util.control.NonFatal

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F]) extends Http4sDsl[F] {

  import Converters._, QueryParams._, Protocol._

  private[http] val prefixPath = "/rates"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? FromQueryParam(from) +& ToQueryParam(to) =>
      rates
        .get(RatesProgramProtocol.GetRatesRequest(from, to))
        .flatMap(Sync[F].fromEither)
        .flatMap { rate =>
          Ok.apply(rate.asGetApiResponse)
        }
        .handleErrorWith {
          case RateLookupFailed(msg) => InternalServerError(msg)
          case NonFatal(error) => InternalServerError(error.getMessage)
        }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
