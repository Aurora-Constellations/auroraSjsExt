package org.aurora.sjsast

import scala.collection.mutable.LinkedHashSet

case class ClinicalCoordinateValue(
  name: String,
  narrative: LinkedHashSet[NL_STATEMENT] = LinkedHashSet.empty,
  refs: LinkedHashSet[RefCoordinate] = LinkedHashSet.empty,
  qu: LinkedHashSet[QU] = LinkedHashSet.empty
)
