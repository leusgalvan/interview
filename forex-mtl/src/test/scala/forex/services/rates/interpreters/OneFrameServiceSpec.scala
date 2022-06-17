package forex.services.rates.interpreters

import cats.effect.{ContextShift, IO}
import forex.domain.Currency
import forex.domain.Rate.Pair
import forex.http.jsonDecoder
import forex.services.rates.Protocol._
import org.http4s.client.blaze.BlazeClientBuilder
import org.scalatest.funsuite.AnyFunSuite

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import fs2._

class OneFrameServiceSpec extends AnyFunSuite {
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)

  // Required local OneFrame docker image running
  test("multiple real calls") {
    val attempts = 1000L
    val result = BlazeClientBuilder[IO](ec).resource.use { client =>
      val service = OneFrameService(client)
      val getRate = service.get(Pair(from = Currency.USD, to = Currency.CAD))
      Stream.repeatEval(getRate).take(attempts).compile.toList
    }.unsafeRunSync()
    assert(result.length == attempts)
  }
}
