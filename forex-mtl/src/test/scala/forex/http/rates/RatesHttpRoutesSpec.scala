package forex.http.rates

import cats.effect._
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.domain.Rate.Pair
import forex.http.rates.Protocol._
import org.scalatest.funsuite.AnyFunSuite
import forex.programs.rates.Algebra
import forex.programs.rates.Protocol._
import forex.programs.rates.errors.Error
import org.http4s.implicits._
import org.http4s._
import Converters._
import forex.programs.rates.errors.Error.RateLookupFailed
import org.http4s.Status.InternalServerError

class RatesHttpRoutesSpec extends AnyFunSuite {
  test("return 200 OK when two valid currencies are provided") {
    val rate = Rate(Pair(Currency.USD, Currency.CAD), Price.fromInt(10), Timestamp.now)
    val ratesProgram = new Algebra[IO] {
      override def get(request: GetRatesRequest): IO[Either[Error, Rate]] =
        IO(Right(rate))
    }
    val routes = new RatesHttpRoutes[IO](ratesProgram)
    val request = Request[IO](method = Method.GET, uri = uri"/rates?from=USD&to=CAD")
    val response = routes.routes.orNotFound.run(request).unsafeRunSync()
    val apiResponse = response.as[GetApiResponse].unsafeRunSync()

    assert(apiResponse == rate.asGetApiResponse)
  }

  test("return 500 Internal Server Error when rates lookup fails with lookup error") {
    val ratesProgram = new Algebra[IO] {
      override def get(request: GetRatesRequest): IO[Either[Error, Rate]] =
        IO(Left(RateLookupFailed("lookup error")))
    }
    val routes = new RatesHttpRoutes[IO](ratesProgram)
    val request = Request[IO](method = Method.GET, uri = uri"/rates?from=USD&to=CAD")
    val response = routes.routes.orNotFound.run(request).unsafeRunSync()
    val body = response.as[String].unsafeRunSync()

    assert(response.status == InternalServerError)
    assert(body == "lookup error")
  }

  test("return 500 Internal Server Error when rates lookup fails with unhandled error") {
    val ratesProgram = new Algebra[IO] {
      override def get(request: GetRatesRequest): IO[Either[Error, Rate]] =
        IO.raiseError(new Exception("unhandled error"))
    }
    val routes = new RatesHttpRoutes[IO](ratesProgram)
    val request = Request[IO](method = Method.GET, uri = uri"/rates?from=USD&to=CAD")
    val response = routes.routes.orNotFound.run(request).unsafeRunSync()
    val body = response.as[String].unsafeRunSync()

    assert(response.status == InternalServerError)
    assert(body == "unhandled error")
  }
}
