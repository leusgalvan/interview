package forex

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.Configuration
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.typelevel.discipline.scalatest.FunSuiteDiscipline

trait BaseSpec
    extends AnyFunSuite
    with Configuration
    with FunSuiteDiscipline
    with Generators
    with ScalaCheckDrivenPropertyChecks
    with Fakes
