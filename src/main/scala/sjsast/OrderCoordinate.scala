package docere.sjsast

import typings.auroraLangium.distTypesSrcLanguageAuroraDiagramGeneratorMod.extractQURefsArray

case class OrderCoordinate (name:String, narratives:Set[String],refs:Set[RefCoordinate]=Set.empty) extends SjsNode:
  def merge (oc:OrderCoordinate):OrderCoordinate = 
    val narratives = this.narratives |+| oc.narratives
    val result = refs |+| oc.refs
    OrderCoordinate(name,narratives,result)

  override def merge(p: SjsNode): SjsNode =
    merge(p.asInstanceOf[OrderCoordinate])

object OrderCoordinate :
  def apply(o: GenAst.OrderCoordinate): OrderCoordinate = 
    val qusrc = extractQURefsArray(o.qurc)
    val refs = qusrc.refs.toList.map{r =>  RefCoordinate(r.$refText)}.toSet
    val narratives = o.narrative.toList.map{n => n.name}.toSet
    OrderCoordinate(o.name,narratives,refs)

