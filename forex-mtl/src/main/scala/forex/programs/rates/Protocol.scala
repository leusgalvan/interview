package forex.programs.rates

import forex.domain.Currency

object Protocol {

  final case class GetRatesRequest(
      from: Currency,
      to: Currency
  ) {
    def inverted: GetRatesRequest = GetRatesRequest(to, from)
  }

}
