package org.aurora.sjsast

import org.aurora.sjsast.{GenAst => G}
import scala.scalajs.js

case class QU(
    query: String =""
)

object QU:
  def fromJs(q: G.QU): QU =
    QU(query = q.query)
  
  def fromJsArray(arr: js.Array[G.QU]): String =
    // Combine all QU query strings into one
    arr.map(_.query).mkString("")