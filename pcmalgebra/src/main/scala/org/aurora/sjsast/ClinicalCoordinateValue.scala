package org.aurora.sjsast

case class ClinicalCoordinateValue(
  name: String,
  narrative: LHSet[NL_STATEMENT] = LHSet(),
  refs: LHSet[RefCoordinate] = LHSet(),
  qu: LHSet[QU] = LHSet()
)
