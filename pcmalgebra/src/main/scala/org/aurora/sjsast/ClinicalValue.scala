package org.aurora.sjsast
 

case class ClinicalValue (name :String, narrative:Set[NL_STATEMENT]=Set.empty, qurefs:QuReferences) extends SjsNode:

  def merge(cv:ClinicalValue):ClinicalValue =
    val narratives = narrative |+| cv.narrative
    val result = qurefs.merge(cv.qurefs)
    ClinicalValue(name, narratives, result)
  override def merge(p: SjsNode): SjsNode = 
    merge(p.asInstanceOf[ClinicalValue])

object ClinicalValue{
  def apply (c: GenAst.ClinicalValue): ClinicalValue = 
    val quref = QuReferences(c.qurc.toOption)
    val narratives = c.narrative.toList.map{n =>  NL_STATEMENT(n.name)}.toSet
    ClinicalValue(c.name, narratives, quref)
}