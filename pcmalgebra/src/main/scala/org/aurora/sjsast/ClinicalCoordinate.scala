package org.aurora.sjsast

case class ClinicalCoordinate(
    name: String,
    narratives: LHSet[NL_STATEMENT] = LHSet(),
    refs: QuReferences = QuReferences(),
    qu: QU = QU("")
)