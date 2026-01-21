package org.aurora.sjsast

import org.aurora.sjsast.{GenAst => G}
import scala.scalajs.js

case class QU(
    query: LHSet[Char] = LHSet()
)

object QU:
  def apply(q: G.QU): QU =
    QU(query = LHSet.from(q.query.toSeq))
  
  def apply(arr: js.Array[G.QU]): QU =
    // Combine all QU query characters into one set
    val allChars = arr.flatMap(_.query.toSeq)
    QU(query = LHSet.from(allChars))