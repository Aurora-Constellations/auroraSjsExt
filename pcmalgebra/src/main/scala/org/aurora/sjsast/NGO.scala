package org.aurora.sjsast

 

case class NGO( name:String, orderCoordinates:Set[OrderCoordinate], narrative:Set[NL_STATEMENT]=Set.empty, quRefs:QuReferences, qu: Set[QU] = Set.empty)   extends SjsNode:
  // Merge logic is in catsgivens.scala, so we don't need to implement it here.
  // def merge(n:NGO):NGO = 
  //   val narratives = narrative |+| n.narrative
  //   val refmerge = quRefs.merge(n.quRefs)
  //   val qumerge = qu |+| n.qu
  //   NGO(name,combine(orderCoordinates,n.orderCoordinates), narratives, refmerge, qumerge)

  override def merge(p: SjsNode): SjsNode =
    import catsgivens.given
    val other = p.asInstanceOf[NGO]
    (Set(this) |+| Set(other)).head


object NGO :
  def apply(n: GenAst.NGO): NGO = 
    val ocoords = n.orders.toList
    .map{o =>  OrderCoordinate(o.asInstanceOf[GenAst.OrderCoordinate])}
    .toSet
    val narratives = n.narrative.toList.map{p =>  NL_STATEMENT(p.name)}.toSet
    val quRefs = QuReferences(n.qurc.toOption)
    val qus = n.qu.toList.map{p =>  QU(p.query)}.toSet
    NGO(n.name,ocoords, narratives, quRefs, qus)    