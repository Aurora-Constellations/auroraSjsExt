package org.aurora.sjsast

import org.aurora.sjsast.{GenAst => G}
import scala.scalajs.js
import scala.collection.mutable.LinkedHashSet

case class QU(
    query: LHSet[Char] = LHSet()
)

object QU:
  def fromJs(q: G.QU): QU =
    QU(query = LinkedHashSet.from(q.query.toSeq))
  
  def fromJsArray(arr: js.Array[G.QU]): QU =
    // Combine all QU query characters into one set
    val allChars = arr.flatMap(_.query.toSeq)
    QU(query = LinkedHashSet.from(allChars))
  
  def toStringQu(qu: QU): String =
    qu.query.mkString("")