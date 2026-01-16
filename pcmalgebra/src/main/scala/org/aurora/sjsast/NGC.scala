package org.aurora.sjsast

case class NGC(
    name: String,
    narratives: LHSet[NL_STATEMENT] = LHSet(),
    coordinates: LHSet[ClinicalCoordinate] = LHSet(), // Ignoring ClinicalValue for simplicity
    refs: QuReferences = QuReferences()
)