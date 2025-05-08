package docere.sjsast

case class ClinicalValue (name :String, narrative:Set[NL_STATEMENT]=Set.empty, refs: Set[RefCoordinate] = Set.empty,qu: Set[QU] = Set.empty) extends SjsNode:

  def merge(cv:ClinicalValue):ClinicalValue =
    val narratives = narrative |+| cv.narrative
    val result = refs |+| cv.refs
    val qumerge = qu |+| cv.qu
    ClinicalValue(name, narratives, result, qumerge)
  override def merge(p: SjsNode): SjsNode = 
    merge(p.asInstanceOf[ClinicalValue])

object ClinicalValue{
  def apply (c: GenAst.ClinicalValue): ClinicalValue = 
    val narratives = c.narrative.toList.map{n =>  NL_STATEMENT(n.name)}.toSet
    val x = c.refs.toList.map { r => RefCoordinate(r.$refText) }.toSet
    val qus = c.qu.toList.map{p =>  QU(p.query)}.toSet
    ClinicalValue(c.name, narratives, x, qus)
}