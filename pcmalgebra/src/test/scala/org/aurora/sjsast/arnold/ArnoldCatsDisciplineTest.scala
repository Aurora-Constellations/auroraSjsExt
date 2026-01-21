package org.aurora.sjsast.arnold

import org.aurora.sjsast._


import org.scalacheck.Prop
import org.typelevel.discipline.Laws

object TruthLaws extends Laws {
  def truth = new DefaultRuleSet(
    name = "truth",
    parent = None,
    "true" -> Prop(_ => Prop.Result(status = Prop.True))
  )
}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.Checkers
import org.typelevel.discipline.scalatest.FunSuiteDiscipline

class TruthSuite extends AnyFunSuite with FunSuiteDiscipline with Checkers {
  checkAll("Truth", TruthLaws.truth)
}



// import cats.laws.discipline.

// // In your test suite (assuming you have the `right Arbitrary and Eq instances)
// class ArnoldCatsDisciplineTest extends AnyFunSuite with FunSuiteDiscipline  with Checkers {
//   checkAll("MyType.BoundedSemiLattice", BoundedSemiLatticeTests[MyType].boundedSemiLattice)
// }