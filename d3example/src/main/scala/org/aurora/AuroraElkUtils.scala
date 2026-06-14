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

object AuroraElkUtils {  

  enum Layout:
    case LAYERED, FORCE, RADIAL

  enum Direction:
    case UP, DOWN

  final case class AuroraElkParameters(
    layout: Layout,
    direction: Direction
  )

  def getDrawableChildren(node: AstNode): js.Array[ElkNode] =
    val children = AstNodeUtils.getAllDescendants(node)
    children.flatMap(c => {
      c match
        case ic: IssueCoordinate => Option(ic.transformToElkNode)
        case oc: OrderCoordinate => Option(oc.transformToElkNode)
        case n: NL_STATEMENT => Option(n.transformToElkNode)  
        case _ => None           
      }).toJSArray

  def getDrawableEdges(node: AstNode): js.Array[ElkExtendedEdge] =
    val children = AstNodeUtils.getAllDescendants(node)
    children.map(c => 
      val sources = AstNodeUtils.getName(c).toJSArray
      val targets = AstNodeUtils.getAllEdges(c).map(AstNodeUtils.getName).toJSArray            
      js.Dynamic.literal(sources = sources, targets = targets).asInstanceOf[ElkExtendedEdge]
      ).toJSArray

}
