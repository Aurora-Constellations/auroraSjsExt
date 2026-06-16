package org.aurora.sjsast.arnold

import org.scalatest._
import wordspec._
import matchers._
import org.aurora.sjsast._

class ArnoldJoinMeet1SyncTest extends BaseSyncTest :
  "type class derivation for Int and String" should {
    "work" in {
      import Join1.{given,*}
      import Show.{given,*}


      case class B(s:String, i: Int,s1:String, s2: String)

      val b1 = B("a", 2, "x","1")
      val b2 = B("b", 3,"y", "2")
      val b3 = B("b", 4, "z", "3")


      info(s"${b1 |+| b2 |+| b3} ")
   }
  }
  

  "typeclass derivation for LHSet" should {
    "work like this" in {
      //TODO FINISH THIS
      // 1. Define the type alias
      import scala.collection.mutable.{LinkedHashSet, LinkedHashMap}

      trait NamedItem:
        val name: String
      type LHSet1[T <: NamedItem] = LinkedHashSet[T]

      case class NamedString(name: String, value: String) extends NamedItem
      case class NamedInt(name: String, value: Int) extends NamedItem

    





    }
  }  