package com.axiom
import testutils.*
import com.axiom.shared.table.*
import org.scalactic.Bool


class ShapelessFieldNameExtractorTest extends AuroraTesting :
  
  
  "Extracting field names" should {
    case class Person(name:String,male:Boolean,age:Int)
    "work like this" in {
         val result = ShapelessFieldNameExtractor.fieldNames[Person]
         result should be(List("name", "male", "age"))
    }
  }
