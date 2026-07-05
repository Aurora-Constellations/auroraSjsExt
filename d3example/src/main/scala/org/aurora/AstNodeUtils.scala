package org.aurora

import org.aurora.sjsast.PCM
import org.aurora.sjsast.{Issues, IssueCoordinate, Clinical, OrderCoordinate, Orders, LHMap, NL_STATEMENT}
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import org.aurora.sjsast.LHSet
import org.aurora.sjsast.AstNode
import org.aurora.sjsast.QuReference
import org.aurora.sjsast.NGO

object AstNodeUtils {

  def getAllDescendants(node: AstNode): LHSet[AstNode] = 
    node match {
      case p: PCM => LHSet.from(p.cio.flatMap((s,c) => getAllDescendants(c)))
      case i: Issues => i.narratives ++ i.ic ++ (i.ic.flatMap(getAllDescendants))
      case ic: IssueCoordinate => ic.narratives ++ ic.qurefs
      case o: Orders => o.narratives ++ o.ngo ++ (o.ngo.flatMap(getAllDescendants))
      case ngo: NGO => ngo.narratives ++ ngo.ordercoord ++ ngo.qurefs ++ (ngo.ordercoord.flatMap(getAllDescendants))
      case oc: OrderCoordinate => oc.narratives ++ oc.qurefs
      case _ => LHSet.empty[AstNode]
    }  

  def getAllEdges(node: AstNode): LHSet[AstNode]= 
    node match {
      case p: PCM => LHSet.from(p.cio.flatMap((s,c) => getAllEdges(c)))
      case ic: IssueCoordinate => LHSet.from(ic.narratives.map(n => n.asInstanceOf[AstNode]))
      case oc: OrderCoordinate => 
        val startingSet = LHSet.from(oc.narratives.map(n => n.asInstanceOf[AstNode]))
        val withRefs = oc.qurefs.flatMap(q => q.qurc).flatMap(r => startingSet.addOne(r)) /* This might cause an issue with the arrow direction -- is there a way to get oc refs from ics?*/
        if withRefs.isEmpty then startingSet else withRefs // want to see if there is a more elegant way to write this
      case _ => LHSet.empty[AstNode]
    }
  
    /* Later on, "name" should just be added as a property of AstNode */
  def getName(a: AstNode): String =
    a match {
      case ic: IssueCoordinate => ic.name + "%%Reference"
      case oc: OrderCoordinate => oc.name + "%%Coordinate"
      case n: NL_STATEMENT => n.name + "%%Statement"
      case q: QuReference => q.refName + "%%Reference"
      case _ => ""
    }
}
