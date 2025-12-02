package org.aurora.sjsast
 
case class OrderCoordinate (name:String, narratives:Set[String],refs:QuReferences) extends SjsNode:
  def merge (oc:OrderCoordinate):OrderCoordinate = 
    val narratives = this.narratives |+| oc.narratives
    val result = refs.merge(oc.refs)
    OrderCoordinate(name,narratives,result)

  override def merge(p: SjsNode): SjsNode =
    merge(p.asInstanceOf[OrderCoordinate])

object OrderCoordinate :
  def apply(o: GenAst.OrderCoordinate): OrderCoordinate = 
    val qurefs = QuReferences(o.qurc.toOption)
    val narratives = o.narrative.toList.map{n => n.name}.toSet
    OrderCoordinate(o.name,narratives,qurefs)

