package com.axiom
import org.scalatest._
import wordspec._
import matchers._

import shapeless3.deriving.*

trait Show[A]:
  def show(a: A): String
object Show:
  given Show[Int] = _.toString
  given Show[Boolean] = _.toString
  given Show[String] = identity(_)
  def deriveShowProduct[A](using
    pInst: K0.ProductInstances[Show, A],
    labelling: Labelling[A]
  ): Show[A] =
    (a: A) =>
      labelling.elemLabels.zipWithIndex
        .map { (label, index) =>
          val value = pInst.project(a)(index)([t] => (st: Show[t], pt: t) => st.show(pt))
          s"$label = $value"
        }
        .mkString(s"${labelling.label}(", ", ", ")")
  def deriveShowSum[A](using
      inst: K0.CoproductInstances[Show, A]
  ): Show[A] =
    (a: A) => inst.fold(a)([a] => (st: Show[a], a: a) => st.show(a))
  inline given derived[A](using gen: K0.Generic[A]): Show[A] =
    gen.derive(deriveShowProduct, deriveShowSum)

class ShapelessShowTest extends AnyWordSpec with should.Matchers{
  "this" should {
    "work" in {
        summon[Show[Int]].show(3) should be("3")
        summon[Show[Option[Int]]].show(Some(3)) should be("Some(value = 3)")  
    }
  }
}
