package org.aurora.sjsast
 
case class OrderCoordinate (name:String, narratives:Set[String],refs:QuReferences) extends SjsNode:
  // Merge logic is in catsgivens.scala, so we don't need to implement it here.
  // def merge (oc:OrderCoordinate):OrderCoordinate = 
  //   val narratives = this.narratives |+| oc.narratives
  //   val result = refs.merge(oc.refs)
  //   OrderCoordinate(name,narratives,result)

  override def merge(p: SjsNode): SjsNode =
    import catsgivens.given
    val other = p.asInstanceOf[OrderCoordinate]
    (Set(this) |+| Set(other)).head

object OrderCoordinate :
  def apply(o: GenAst.OrderCoordinate): OrderCoordinate = 
    val qurefs = QuReferences(o.qurc.toOption)
    val narratives = o.narrative.toList.map{n => n.name}.toSet
    OrderCoordinate(o.name,narratives,qurefs)

