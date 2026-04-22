package org.aurora.sjsast

import scala.scalajs.js

case class NGC(
    name: String,
    narratives: LHSet[NL_STATEMENT] = LHSet(),
    coordinates: LHSet[ClinicalItem] = LHSet(),
    refs: LHSet[QuReferences] = LHSet()
)

object NGC:
  def apply(ngc: GenAst.NGC): NGC =
    val narratives = LHSet(ngc.narrative.toList.map(NL_STATEMENT.apply)*)

    val coords = LHSet(
      ngc.coord.toList.map { x =>
        ClinicalItem(x.asInstanceOf[GenAst.ClinicalItem])
      }*
    )

    val refs = ngc.qurc.toOption match {
      case Some(qrs) => LHSet(QuReferences(qrs))
      case None      => LHSet()
    }

    NGC(
      name = ngc.name,
      narratives = narratives,
      coordinates = coords,
      refs = refs
    )