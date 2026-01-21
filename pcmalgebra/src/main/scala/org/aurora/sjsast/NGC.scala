package org.aurora.sjsast

case class NGC(
    name: String,
    narratives: LHSet[NL_STATEMENT] = LHSet(),
    coordinates: LHSet[ClinicalCoordinate] = LHSet(), // Ignoring ClinicalValue for simplicity
    refs: LHSet[QuReferences] = LHSet()
)

object NGC:
  def apply(ngc: GenAst.NGC): NGC =
    val name = ngc.name
    val narratives = LHSet(ngc.narrative.toList.map(NL_STATEMENT(_))*)
    val coords =
      LHSet(
          ngc.coord.toList
          .filter(_.$type == "ClinicalCoordinate")
          .map { x =>
              ClinicalCoordinate(x.asInstanceOf[GenAst.ClinicalCoordinate])
          }*
      )
    val refs = ngc.qurc.toOption match {
      case Some(qrs) => LHSet(QuReferences(qrs))
      case None => LHSet()
    }
    NGC(
      name = name,
      narratives = narratives,
      coordinates = coords,
      refs = refs
    )