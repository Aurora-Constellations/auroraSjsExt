package org.aurora.sjsast

case class NGO(
    name: String,
    narratives: LHSet[NL_STATEMENT] = LHSet(),
    orders: LHSet[OrderCoordinate] = LHSet(), // Assuming simplified structure, ignoring MutuallyExclusive for now
    refs: QuReferences = QuReferences(),
    qu: LHSet[QU] = LHSet()
)