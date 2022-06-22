package forex

import scala.concurrent.ExecutionContext
import cats.effect._
import dev.profunktor.redis4cats.connection.{RedisClient, RedisURI}
import forex.config._
import fs2.Stream
import org.http4s.server.blaze.BlazeServerBuilder
import dev.profunktor.redis4cats.effect.Log.NoOp._
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    new Application[IO].stream(executionContext).compile.drain.as(ExitCode.Success)

}

class Application[F[_]: ConcurrentEffect: Timer: ContextShift] {
  private def makeResources(config: ApplicationConfig, ec: ExecutionContext): Resource[F, (RedisClient, Client[F])] = {
    for {
      redisURI <- Resource.eval(RedisURI.make[F](
        s"redis://${config.redis.host}:${config.redis.port}")
      )
      redisClient <- RedisClient[F].fromUri(redisURI)
      httpClient <- BlazeClientBuilder[F](ec).resource
    } yield (redisClient, httpClient)
  }

  def stream(ec: ExecutionContext): Stream[F, Unit] =
    for {
      config <- Config.stream("app")
      (redisClient, httpClient) <- Stream.resource(makeResources(config, ec))
      module = new Module[F](config, redisClient, httpClient)
      _ <- BlazeServerBuilder[F](ec)
            .bindHttp(config.http.port, config.http.host)
            .withHttpApp(module.httpApp)
            .serve
    } yield ()
}
