package org.aurora.sjsast 

case class ClinicalCoordinate (name :String, narrative:Set[NL_STATEMENT]=Set.empty, refs: QuReferences) extends SjsNode:

  def merge(cc:ClinicalCoordinate):ClinicalCoordinate = 
    val narratives = narrative |+| cc.narrative
    val result = refs.merge(cc.refs)
    // val qumerge = qu |+| cc.qu
    ClinicalCoordinate(name, narratives, result)
  override def merge(p: SjsNode): SjsNode = 
    merge(p.asInstanceOf[ClinicalCoordinate])

object ClinicalCoordinate{
  import scala.scalajs.js
  import js.JSConverters._

  def apply (c: GenAst.ClinicalCoordinate): ClinicalCoordinate = 
    val narratives = c.narrative.toList.map{n =>  NL_STATEMENT(n.name)}.toSet

    val qurefs = QuReferences(c.qurc.toOption)
    ClinicalCoordinate(c.name, narratives, qurefs)
}