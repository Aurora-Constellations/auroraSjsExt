package org.aurora.sjsast

case class IssueCoordinate(
    name: String,
    narratives: LHSet[NL_STATEMENT] = LHSet(),
    qurefs: QuReferences = QuReferences(),
    qu: QU = QU()
)