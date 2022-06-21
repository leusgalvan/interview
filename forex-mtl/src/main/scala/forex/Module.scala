package forex

import cats.effect.{Concurrent, ContextShift, Timer}
import dev.profunktor.redis4cats.connection.RedisClient
import forex.config.ApplicationConfig
import forex.http.rates.RatesHttpRoutes
import forex.services._
import forex.programs._
import forex.services.redis.interpreters.RedisService
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.middleware.{AutoSlash, Timeout}

class Module[F[_]: Concurrent: Timer: ContextShift](config: ApplicationConfig, redisClient: RedisClient) {
  private val redisService: RedisService[F] = RedisServices.live[F](redisClient)

  private val ratesService: RatesService[F] = RatesServices.dummy[F]

  private val ratesProgram: RatesProgram[F] = RatesProgram[F](ratesService, redisService)

  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes

  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  private val routesMiddleware: PartialMiddleware = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    }
  }

  private val appMiddleware: TotalMiddleware = { http: HttpApp[F] =>
    Timeout(config.http.timeout)(http)
  }

  private val http: HttpRoutes[F] = ratesHttpRoutes

  val httpApp: HttpApp[F] = appMiddleware(routesMiddleware(http).orNotFound)

}
