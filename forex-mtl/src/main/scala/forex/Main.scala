package forex

import cats.Parallel

import scala.concurrent.ExecutionContext
import cats.effect._
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import dev.profunktor.redis4cats.connection.{RedisClient, RedisURI}
import dev.profunktor.redis4cats.data.RedisCodec
import dev.profunktor.redis4cats.log4cats._
import forex.config._
import fs2.Stream
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.typelevel.log4cats._
import org.typelevel.log4cats.slf4j._

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    implicit val logger: Logger[IO] = LoggerFactory[IO].getLogger

    new Application[IO].stream(executionContext).compile.drain.as(ExitCode.Success)
  }

}

class Application[F[_]: ConcurrentEffect: Timer: ContextShift: Logger: Parallel] {
  private def makeResources(config: ApplicationConfig, ec: ExecutionContext): Resource[F, (RedisCommands[F, String, String], Client[F])] = {
    for {
      redisURI <- Resource.eval(RedisURI.make[F](
        s"redis://${config.redis.host}:${config.redis.port}")
      )
      redisClient <- RedisClient[F].fromUri(redisURI)
      redisCommands <- Redis[F].fromClient(redisClient, RedisCodec.Utf8)
      httpClient <- BlazeClientBuilder[F](ec).resource
    } yield (redisCommands, httpClient)
  }

  def stream(ec: ExecutionContext): Stream[F, Unit] = {
    for {
      config <- Config.stream("app")
      (redisCommands, httpClient) <- Stream.resource(makeResources(config, ec))
      module = new Module[F](config, redisCommands, httpClient)
      _ <- BlazeServerBuilder[F](ec)
            .bindHttp(config.http.port, config.http.host)
            .withHttpApp(module.httpApp)
            .serve
    } yield ()
  }
}
