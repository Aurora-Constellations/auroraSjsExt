package org.aurora

import org.aurora.sjsast.PCM
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import typings.elkjs.libElkApiMod.ElkNode
import org.aurora.sjsast.IssueCoordinate
import org.aurora.sjsast.OrderCoordinate
import org.aurora.sjsast.NL_STATEMENT
import typings.elkjs.libElkApiMod.ElkExtendedEdge
import org.aurora.sjsast.AstNode
import org.aurora.sjsast.LHSet

object AuroraElkUtils {  

  def getDrawableDescendants(node: AstNode): LHSet[ElkNode] =
    val children = AstNodeUtils.getAllDescendants(node)
    children.flatMap(c => {
      c match
        case ic: IssueCoordinate => Option(ic.transformToElkNode)
        case oc: OrderCoordinate => Option(oc.transformToElkNode)
        case n: NL_STATEMENT => Option(n.transformToElkNode)  
        case _ => None           
      })

  def getDrawableEdges(node: AstNode): LHSet[ElkExtendedEdge] =
    val nodeAndChildren = AstNodeUtils.getAllDescendants(node).addOne(node)
    nodeAndChildren.flatMap(c => 
      /* for layouting purposes, each edge must have one source and one target */
      val source = AstNodeUtils.getName(c)
      val targetNodes = AstNodeUtils.getAllEdges(c)
      
      targetNodes.map(targetNode => {
        val targetName = AstNodeUtils.getName(targetNode)
        val qualifier = AstNodeUtils.getQualifier(targetNode)
        
        // Inject the strictly typed edge class into the ELK edge ID
        ElkExtendedEdge(
          id = s"${source}->${targetName}%%${qualifier.elkType}", // D3 will receive something like this OC1->IC1%%DraftEdge
          sources = js.Array(source), 
          targets = js.Array(targetName)
        )
      })       
      )

}
