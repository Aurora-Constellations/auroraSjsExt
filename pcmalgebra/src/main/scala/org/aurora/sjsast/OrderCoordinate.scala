package org.aurora.sjsast

import scala.scalajs.js
//TODO common trait Named so that we can generalize typeclass derivations for LHSet[T <: Named]??
case class OrderCoordinate(
    name: String,
    narratives: LHSet[NL_STATEMENT] = LHSet(),
    qurefs: LHSet[QuReferences] = LHSet()
) extends RefCoordinate

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