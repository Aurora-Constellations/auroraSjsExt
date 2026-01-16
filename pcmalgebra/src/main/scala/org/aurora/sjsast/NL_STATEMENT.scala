package org.aurora.sjsast

import org.aurora.sjsast.{GenAst => G}
import scala.scalajs.js
import scala.collection.mutable.LinkedHashSet

case class NL_STATEMENT(
    name: String
)

object NL_STATEMENT:
  def fromJs(n: G.NL_STATEMENT): NL_STATEMENT = 
    // Access the 'name' property dynamically to be safe
    val rawName = n.asInstanceOf[js.Dynamic].selectDynamic("name").toString
    NL_STATEMENT(rawName)
    
  def fromJsSeq(seq: Seq[G.NL_STATEMENT]): LHSet[NL_STATEMENT] =
    // FIX: Use LinkedHashSet.from
    LinkedHashSet.from(seq.map(fromJs))