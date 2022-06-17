package forex.programs.rates

import cats.effect.IO
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.domain.Rate.Pair
import forex.programs.rates.Protocol.GetRatesRequest
import forex.services.RatesService
import forex.services.rates.Algebra
import forex.services.rates.errors.Error
import org.scalatest.funsuite.AnyFunSuite

class ProgramSpec extends AnyFunSuite {
  test("return rate given by service") {
    val rate = Rate(Pair(Currency.USD, Currency.CAD), Price.fromInt(10), Timestamp.now)
    val service: RatesService[IO] = new Algebra[IO] {
      override def get(pair: Pair): IO[Either[Error, Rate]] =
        IO(Right(rate))
    }
    val program = Program[IO](service)
    val request = GetRatesRequest(from = Currency.USD, to = Currency.CAD)
    val result = program.get(request).unsafeRunSync()
    assert(result == Right(rate))
  }
}
