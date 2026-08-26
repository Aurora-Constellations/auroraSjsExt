package org.aurora.sjsast
 
import scala.scalajs.js

sealed trait RefCoordinate extends AstNode{
    def name: String
} 

object RefCoordinate:
  def apply(ast: GenAst.ReferenceCoordinate): RefCoordinate =
    val node = ast.asInstanceOf[js.Dynamic]
    node.`$type`.asInstanceOf[String] match {
      case "ClinicalItem" =>
        ClinicalItem(ast.asInstanceOf[GenAst.ClinicalItem])
      case "IssueCoordinate"    => 
        IssueCoordinate(ast.asInstanceOf[GenAst.IssueCoordinate])
      case "OrderCoordinate"    => 
        OrderCoordinate(ast.asInstanceOf[GenAst.OrderCoordinate])
      case unknown => 
        throw new Exception(s"Unsupported coordinate type: $unknown")
    }

case class ClinicalItem(
  name: String,
  narratives: LHSet[NL_STATEMENT] = LHSet(),
  qurefs: LHSet[QuReferences] = LHSet(),
  qu: QU = QU(),
  values: List[SingleValueUnit] = List()
) extends RefCoordinate

object ClinicalItem:
  def apply(cc: GenAst.ClinicalItem): ClinicalItem = 
    val name = cc.name
    val narratives = LHSet(cc.narrative.toList.map(NL_STATEMENT(_))*)
    val qurefs = LHSet(cc.qurc.toList.map(QuReferences(_))*)
    val qu = QU(cc.qu)
    val values =
      if js.isUndefined(cc.values) then List()
      else cc.values.toList.map(SingleValueUnit.apply)
    ClinicalItem(name, narratives, qurefs, qu, values)

case class IssueCoordinate(
  name: String,
  fromMods: List[String] = List(),
  narratives: LHSet[NL_STATEMENT] = LHSet(),
  qurefs: LHSet[QuReferences] = LHSet(),
  qu: QU = QU()
) extends RefCoordinate

object IssueCoordinate:
  def apply(ic: GenAst.IssueCoordinate): IssueCoordinate = 
    val name = ic.name
    val mods = ic.mods.toList.flatMap { m =>
      val refText = m.asInstanceOf[js.Dynamic].selectDynamic("$refText")
      if (refText != js.undefined) Some(refText.asInstanceOf[String]) else None
    }
    val narratives = LHSet(ic.narrative.toList.map(NL_STATEMENT(_))*)
    val qurefs = LHSet(ic.qurc.toList.map(QuReferences(_))*)
    val qu = QU(ic.qu)
    IssueCoordinate(name, mods, narratives, qurefs, qu)

case class OrderCoordinate(
  name: String,
  narratives: LHSet[NL_STATEMENT] = LHSet(),
  qurefs: LHSet[QuReferences] = LHSet()
) extends RefCoordinate

object OrderCoordinate:
  def apply(oc: GenAst.OrderCoordinate): OrderCoordinate =
    val name = oc.name
    val narratives = LHSet(oc.narrative.toList.map(NL_STATEMENT(_))*)
    val refs = oc.qurc.toOption match {
      case Some(qrs) => LHSet(QuReferences(qrs))
      case None => LHSet()
    }
    
    OrderCoordinate(
      name = name, 
      narratives = narratives, 
      qurefs = refs
    )
