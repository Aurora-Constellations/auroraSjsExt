package org.aurora.sjsast
 
case class Orders(ngo:Set[NGO], narrative:Set[NL_STATEMENT]=Set.empty)//  extends SjsNode :

object Orders :
  def apply(o:GenAst.Orders)  : Orders = 
    val ng = o.namedGroups.toList.map{NGO(_)}.toSet
    val narratives = o.narrative.toList.map{n =>  NL_STATEMENT(n.name)}.toSet
    Orders(ng, narratives)