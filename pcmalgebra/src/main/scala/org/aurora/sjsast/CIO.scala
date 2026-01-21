package org.aurora.sjsast

// Sealed trait and its children must be in the same file
sealed trait CIO

case class Clinical(
    name: String = "Clinical",
    narratives: LHSet[NL_STATEMENT] = LHSet(),
    ngc: LHSet[NGC] = LHSet()
) extends CIO

object Clinical:
  def apply(o: GenAst.Clinical): Clinical =
    val ngc = LHSet(o.namedGroups.toList.map(NGC(_))*)
    val narratives = LHSet(o.narrative.toList.map(NL_STATEMENT(_))*) //* here means “expand this collection into individual arguments”.
    Clinical(narratives = narratives, ngc = ngc)

case class Orders(
    name: String = "Orders",
    narratives: LHSet[NL_STATEMENT] = LHSet(),
    ngo: LHSet[NGO] = LHSet()
) extends CIO

object Orders:
  def apply(o: GenAst.Orders): Orders =
    val ngo = LHSet(o.namedGroups.toList.map(NGO(_))*)
    val narratives = LHSet(o.narrative.toList.map(NL_STATEMENT(_))*) //* here means “expand this collection into individual arguments”.
    Orders(narratives = narratives, ngo = ngo)

case class Issues(
    name: String = "Issues",
    narratives: LHSet[NL_STATEMENT] = LHSet(),
    ic: LHSet[IssueCoordinate] = LHSet()
) extends CIO

object Issues:
  def apply(i: GenAst.Issues): Issues =
    val coords = LHSet(i.coord.toList.map(IssueCoordinate(_))*)
    val narratives = LHSet(i.narrative.toList.map(NL_STATEMENT(_))*) //* here means “expand this collection into individual arguments”.
    Issues(narratives = narratives, ic = coords)