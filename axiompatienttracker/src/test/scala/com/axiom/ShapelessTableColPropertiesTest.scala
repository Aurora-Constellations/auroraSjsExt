package com.axiom
import testutils.*

import shapeless3.deriving.*

class ShapelessTableColPropertiesTest extends AuroraTesting:
  "this" should {
    "work" in {
      case class Person(name:String, age: Int, exists:Option[Boolean])
      val p  = Person("arnold",58,Some(true))
      val list = List(p,p,p)

      val result = list.map { p =>
        TableColProperties.derived[Person].element(p)
      }
       

      info(s"result: $result")

    }
  }
