package forex

import cats.effect.{Concurrent, Timer}
import dev.profunktor.redis4cats.RedisCommands
import forex.config.ApplicationConfig
import forex.http.rates.RatesHttpRoutes
import forex.services._
import forex.programs._
import org.http4s._
import org.http4s.client.Client
import org.http4s.implicits._
import org.http4s.server.middleware.{AutoSlash, Timeout, Logger => ServerLogMiddleware}
import org.http4s.client.middleware.{Retry, RetryPolicy, Logger => ClientLogMiddleware}
import org.typelevel.log4cats.Logger

class Module[F[_]: Concurrent: Timer: Logger](
    config: ApplicationConfig,
    redisCommands: RedisCommands[F, String, String],
    httpClient: Client[F]
) {

  private val redisService: RedisService[F] = RedisServices.live[F](redisCommands, config.redis.expiration)

  private val oneFrameClient = {
    val withRetries = (client: Client[F]) =>
      Retry[F](RetryPolicy(backoff = RetryPolicy.exponentialBackoff(config.oneFrame.maxWaitRetry, config.oneFrame.maxRetries))
    )(client)
    val withLogging = (client: Client[F]) => ClientLogMiddleware(logHeaders = false, logBody = true)(client)

    withRetries(withLogging(httpClient))
  }

  private val ratesService: RatesService[F] = RatesServices.live[F](
    oneFrameClient,
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
