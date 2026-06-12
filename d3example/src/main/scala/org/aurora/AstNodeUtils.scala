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
      case i: Issues => i.narratives ++ i.ic
      case ic: IssueCoordinate => ic.narratives ++ ic.qurefs
      case o: Orders => o.narratives ++ o.ngo
      case ngo: NGO => ngo.narratives ++ ngo.ordercoord ++ ngo.qurefs
      case oc: OrderCoordinate => oc.narratives ++ oc.qurefs
      case q: QuReference => LHSet.empty[AstNode]
      case n: NL_STATEMENT => LHSet.empty[AstNode]
    }  
}
