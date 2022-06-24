package forex.services.rates.interpreters

import cats.effect.{ContextShift, IO}
import forex.domain.Currency
import forex.domain.Rate.Pair
import org.http4s.client.blaze.BlazeClientBuilder
import org.scalatest.funsuite.AnyFunSuite

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import fs2._

class OneFrameServiceSpec extends AnyFunSuite {
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)
  val host = "localhost"
  val port = 8080
  val token = "10dc303535874aeccc86a8251e6992f5"

  // Required local OneFrame docker image running
  test("multiple real calls") {
    val attempts = 1001L // ensure quota is reached
    val result = BlazeClientBuilder[IO](ec).resource.use { client =>
      val service = OneFrameService(client, host, port, token)
      val getRate = service.get(Pair(from = Currency.USD, to = Currency.CAD))
      Stream.repeatEval(getRate).take(attempts).compile.toList
    }.unsafeRunSync()
    assert(result.length == attempts)
  }
}
