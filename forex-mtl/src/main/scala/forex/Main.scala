package forex

import scala.concurrent.ExecutionContext
import cats.effect._
import dev.profunktor.redis4cats.connection.{RedisClient, RedisURI}
import forex.config._
import fs2.Stream
import org.http4s.server.blaze.BlazeServerBuilder
import dev.profunktor.redis4cats.effect.Log.NoOp._

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    new Application[IO].stream(executionContext).compile.drain.as(ExitCode.Success)

}

class Application[F[_]: ConcurrentEffect: Timer: ContextShift] {
  private def makeRedis(config: ApplicationConfig): Resource[F, RedisClient] = {
    for {
      uri <- Resource.eval(RedisURI.make[F](
        s"redis://${config.redis.host}:${config.redis.port}")
      )
      client <- RedisClient[F].fromUri(uri)
    } yield client
  }

  def stream(ec: ExecutionContext): Stream[F, Unit] =
    for {
      config <- Config.stream("app")
      redisClient <- Stream.resource(makeRedis(config))
      module = new Module[F](config, redisClient)
      _ <- BlazeServerBuilder[F](ec)
            .bindHttp(config.http.port, config.http.host)
            .withHttpApp(module.httpApp)
            .serve
    } yield ()
}
