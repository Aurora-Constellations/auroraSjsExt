package com.axiom

import testutils.*
import shapeless3.deriving.*

import scala.deriving.*

object Tuples:
  def to[A <: Product](value: A)(
    using mirror: Mirror.ProductOf[A]
  ): mirror.MirroredElemTypes = Tuple.fromProductTyped(value)

  def from[A](value: Product)(
    using
    mirror: Mirror.ProductOf[A],
    ev: value.type <:< mirror.MirroredElemTypes
  ): A = mirror.fromProduct(value)


class Scala3CaseClassTupleTest extends AuroraTesting{
  "this" should {
    "work" in {

      case class SBI(s:String,b:Boolean,i:Int)
      
      val t = ("hello",true,1)
      val sbi = Tuples.from[SBI](t)

      Tuples.to[SBI](sbi) should be(t)
    }
  }
}
