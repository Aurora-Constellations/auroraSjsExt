package org.aurora.sjsast

import scala.collection.mutable.LinkedHashSet

// Sealed trait and its children must be in the same file
sealed trait CIO

case class Clinical(
    name: String = "Clinical",
    narratives: LHSet[NL_STATEMENT] = LHSet(),
    namedGroups: LHSet[NGC] = LHSet()
) extends CIO

case class Orders(
    name: String = "Orders",
    narratives: LHSet[NL_STATEMENT] = LHSet(),
    namedGroups: LHSet[NGO] = LHSet()
) extends CIO

case class Issues(
    name: String = "Issues",
    narratives: LHSet[NL_STATEMENT] = LHSet(),
    coordinates: LHSet[IssueCoordinate] = LHSet()
) extends CIO