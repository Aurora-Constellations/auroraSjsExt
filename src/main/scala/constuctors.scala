package docere

import docere.sjsast.*
object contructors:
  val issueCoordsX = List("a", "b")
  val issueCoordsY = List("b", "c")

  val ref1X = List("a")
  val ref2X = List("a", "b")

  val ref1Y = List("b")
  val ref2Y = List("b", "c")

  val ngoX = List("NGOX1", "NGOX2")
  val ngoY = List("NGOX1", "NGOY2")

  def refs(names: Set[String]) = names.toSet.map { RefCoordinate(_) }
  def ocoords(names: Set[String], refs: Set[RefCoordinate]) = names.toSet.map { x => OrderCoordinate(x,Set.empty, refs) }
  def icoords(names: Set[String], refs: Set[RefCoordinate]) = names.toSet.map { x => IssueCoordinate(x,Set.empty) }
  def ccoords(names: Set[String], narrative:Set[NL_STATEMENT],refs: Set[RefCoordinate]) = names.toSet.map { x => ClinicalCoordinate(x, narrative, refs) }