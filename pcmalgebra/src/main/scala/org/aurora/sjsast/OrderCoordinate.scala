package org.aurora.sjsast

import org.aurora.sjsast.{GenAst => G}
import scala.scalajs.js
import scala.collection.mutable.LinkedHashSet

case class OrderCoordinate(
    name: String,
    narratives: LHSet[NL_STATEMENT] = LHSet(),
    qurefs: QuReferences = QuReferences()
)

object OrderCoordinate:
  def apply(oc: G.OrderCoordinate): OrderCoordinate =
    val name = oc.name
    val narratives = NL_STATEMENT.fromJsSeq(oc.narrative.toSeq)
    val refs = oc.qurc.toOption match {
      case Some(qrs) => QuReferences(qrs)
      case None => QuReferences()
    }
    
    OrderCoordinate(
      name = name, 
      narratives = narratives, 
      qurefs = refs
    )