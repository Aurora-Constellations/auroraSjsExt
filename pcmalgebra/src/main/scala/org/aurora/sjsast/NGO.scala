package org.aurora.sjsast

case class NGO(
    name: String,
    narratives: LHSet[NL_STATEMENT] = LHSet(),
    ordercoord: LHSet[OrderCoordinate] = LHSet(), // Assuming simplified structure, ignoring MutuallyExclusive for now
    qurefs: QuReferences = QuReferences(),
    qu: LHSet[QU] = LHSet()
)