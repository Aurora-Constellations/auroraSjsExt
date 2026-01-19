package org.aurora.sjsast

import org.aurora.sjsast.{GenAst => G}
import scala.scalajs.js
import scala.collection.mutable.LinkedHashSet

case class OrderCoordinate(
    name: String,
    narratives: LHSet[NL_STATEMENT] = LHSet(),
    refs: QuReferences = QuReferences(),
)

object OrderCoordinate:
  def fromJs(oc: G.OrderCoordinate): OrderCoordinate =
    val name = oc.name
    val narratives = NL_STATEMENT.fromJsSeq(oc.narrative.toSeq)
    val refs = oc.qurc.toOption match {
      case Some(qrs) => QuReferences.fromJs(qrs)
      case None => QuReferences()
    }
    val qu = LinkedHashSet.from(oc.qu.toSeq.map(QU.fromJs))
    val comments = LinkedHashSet.from(oc.comment.toSeq)
    
    OrderCoordinate(
      name = name, 
      narratives = narratives, 
      refs = refs
    )