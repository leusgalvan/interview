package forex.config

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(
    http: HttpConfig,
    redis: RedisConfig,
    oneFrame: OneFrameConfig
)

case class HttpConfig(
    host: String,
    port: Int,
    timeout: FiniteDuration
)

case class RedisConfig(
    host: String,
    port: Int,
    expiration: FiniteDuration
)

case class OneFrameConfig(
    host: String,
    port: Int,
    token: String,
    maxRetries: Int,
    maxWaitRetry: FiniteDuration
)