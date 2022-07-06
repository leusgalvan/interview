package forex.services.rates.interpreters

import cats.effect.{ContextShift, IO}
import forex.BaseSpec
import forex.domain.Rate.Pair
import org.http4s.client.blaze.BlazeClientBuilder

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import org.scalacheck.Gen

class OneFrameServiceSpec extends BaseSpec {
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)
  val host = "localhost"
  val port = 8080
  val token = "10dc303535874aeccc86a8251e6992f5"

  // Required local OneFrame docker image running
  val pairsGen: Gen[List[Pair]] = Gen.atLeastOne(Pair.all).map(_.toList)
  test("multiple real calls") {
    forAll(pairsGen) { (pairs: List[Pair]) =>
      val result = BlazeClientBuilder[IO](ec).resource.use { client =>
        val service = OneFrameService(client, host, port, token)
        service.get(pairs)
      }.unsafeRunSync()
      println(result)
    }
  }
}
