package org.aurora.sjsast

import scala.scalajs.js

case class OrderCoordinate(
    name: String,
    narratives: LHSet[NL_STATEMENT] = LHSet(),
    qurefs: LHSet[QuReferences] = LHSet()
)

object OrderCoordinate:
  def apply(oc: GenAst.OrderCoordinate): OrderCoordinate =
    val name = oc.name
    val narratives = LHSet(oc.narrative.toList.map(NL_STATEMENT(_))*)
    val refs = oc.qurc.toOption match {
      case Some(qrs) => LHSet(QuReferences(qrs))
      case None => LHSet()
    }
    
    OrderCoordinate(
      name = name, 
      narratives = narratives, 
      qurefs = refs
    )