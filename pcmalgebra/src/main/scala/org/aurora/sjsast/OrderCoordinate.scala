package org.aurora.sjsast

case class OrderCoordinate(
    name: String,
    narratives: LHSet[NL_STATEMENT] = LHSet(),
    refs: QuReferences = QuReferences(),
)