package com.axiom

import org.scalatest._
import wordspec._
import matchers._
import scala.util.Try



class IndexedSeqTest extends AnyWordSpec with should.Matchers {
  "this" should {
    "work" in {

    val ll =   List(List(0,1,2,3,4),
                    List(0,1,2,3,4)
                   )
    def toIndexedSeq(l : List[List[Int]]): IndexedSeq[IndexedSeq[Int]] = l.map{_.toIndexedSeq}.toIndexedSeq

    val ssl = toIndexedSeq(ll)

    case class Coord(col:Int,row:Int)

    extension (ll:IndexedSeq[IndexedSeq[Int]]) 
      def colRow(col:Int,row:Int) = Try( ll(row)(col)).toOption
      def coord(c:Coord) = Try( ll(c.row)(c.col)).toOption
      
      
    ssl.colRow(0,3) should be(None)

    info(s"${ssl.zip(ssl)}")
    }

  }
}