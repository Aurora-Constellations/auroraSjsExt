package org.aurora.sjsast

case class ClinicalCoordinate(
    name: String,
    narratives: LHSet[NL_STATEMENT] = LHSet(),
    qurefs: LHSet[QuReferences] = LHSet(),
    qu: QU = QU()
)

object ClinicalCoordinate:
    def apply(cc: GenAst.ClinicalCoordinate): ClinicalCoordinate = 
        val name = cc.name
        val narratives = LHSet(cc.narrative.toList.map(NL_STATEMENT(_))*)
        val qurefs = LHSet(cc.qurc.toList.map(QuReferences(_))*)
        val qu = QU(cc.qu)
        ClinicalCoordinate(name, narratives, qurefs, qu)