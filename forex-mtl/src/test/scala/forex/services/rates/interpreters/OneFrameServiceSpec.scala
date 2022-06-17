package forex.services.rates.interpreters

import cats.effect.{ContextShift, IO}
import forex.domain.Currency
import forex.domain.Rate.Pair
import forex.http.jsonDecoder
import org.http4s.client.blaze.BlazeClientBuilder
import org.scalatest.funsuite.AnyFunSuite

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

class OneFrameServiceSpec extends AnyFunSuite {
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)

  test("simple call") {
    val result = BlazeClientBuilder[IO](ec).resource.use { client =>
      val service = OneFrameService(client)
      service.get(Pair(from = Currency.USD, to = Currency.CAD))
    }.unsafeRunSync()

    assert(result.isRight)
  }
}
