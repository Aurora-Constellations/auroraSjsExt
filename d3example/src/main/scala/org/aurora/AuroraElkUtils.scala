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

  enum Layout:
    case LAYERED, FORCE, RADIAL

  enum Direction:
    case UP, DOWN

  final case class AuroraElkParameters(
    layout: Layout,
    direction: Direction
  )

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
      val sources = AstNodeUtils.getName(c).toJSArray
      val targets = AstNodeUtils.getAllEdges(c).map(AstNodeUtils.getName).toJSArray    
      targets.isEmpty match {
        case true => None
        case _ => Option(js.Dynamic.literal(sources = sources, targets = targets).asInstanceOf[ElkExtendedEdge])
        }        
      )

}
