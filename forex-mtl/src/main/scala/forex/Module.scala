package forex

import cats.effect.{Concurrent, ContextShift, Timer}
import dev.profunktor.redis4cats.connection.RedisClient
import forex.config.ApplicationConfig
import forex.http.rates.RatesHttpRoutes
import forex.services._
import forex.programs._
import org.http4s._
import org.http4s.client.Client
import org.http4s.implicits._
import org.http4s.server.middleware.{AutoSlash, Timeout, Logger => ServerLogMiddleware}
import org.http4s.client.middleware.{Logger => ClientLogMiddleware}
import org.typelevel.log4cats.Logger

class Module[F[_]: Concurrent: Timer: ContextShift: Logger](
    config: ApplicationConfig,
    redisClient: RedisClient,
    httpClient: Client[F]
) {

  private val redisService: RedisService[F] = RedisServices.live[F](redisClient, config.redis.expiration)

  private val ratesService: RatesService[F] = RatesServices.live[F](
    ClientLogMiddleware(logHeaders = false, logBody = true)(httpClient),
    config.oneFrame.host,
    config.oneFrame.port
  )

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
    ServerLogMiddleware.httpApp(logHeaders = false, logBody = true)(
      Timeout(config.http.timeout)(http)
    )
  }

  private val http: HttpRoutes[F] = ratesHttpRoutes

  val httpApp: HttpApp[F] = appMiddleware(routesMiddleware(http).orNotFound)

}
