package com.axiom
import org.scalatest._
import wordspec._
import matchers._

import shapeless3.deriving.*
import org.scalactic.Bool


class ShapelessFieldNameExtractorTest extends AnyWordSpec with should.Matchers{
  "Extracting field names" should {
    case class Person(name:String,male:Boolean,age:Int)
    "work like this" in {
         val result = ShapelessFieldNameExtractor.fieldNames[Person]
         result should be(List("name", "male", "age"))
    }
  }
}
