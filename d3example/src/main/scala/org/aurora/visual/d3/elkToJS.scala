package org.aurora.visual.d3

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import org.aurora.sjsast.*
import org.aurora.sjsast.utils.*
import typings.vscodeLanguageserverProtocol.libCommonProtocolMod._InitializeParams
import org.aurora.visual.elk.AuroraElk
import com.axiom.protocol.{DiagramEdge, DiagramNode, EdgeSection, Point, SourceRange}

object AstTransformer {
  
  def fromElkToD3Node(
    elkNode: AuroraElk.Node,
    pcm: PCM,
    ranges: Map[String, SourceRange] = Map.empty
  ): DiagramNode = {
    val allAstNodes = AstNode.getAllDescendants(pcm).toList
    buildTree(elkNode, allAstNodes, ranges)
  }

  private def buildTree(
    elkNode: AuroraElk.Node,
    allAstNodes: List[AstNode],
    ranges: Map[String, SourceRange]
  ): DiagramNode = {
    
    // In AuroraElk.scala, we see the ID often contains "%%"
    // We can extract the raw name by taking the first part of that ID.
    val nodeName = elkNode.id.split("%%").headOption.getOrElse("Unknown")
    val astNode = allAstNodes.find {
      case ic: IssueCoordinate => elkNode.nodeType == "Reference" && ic.name == nodeName
      case oc: OrderCoordinate => elkNode.nodeType == "Coordinate" && oc.name == nodeName
      case _ => false
    }
    val qualifier = astNode.map(AstNode.getQualifier).getOrElse(Qualifier.Normal).elkType
    
    DiagramNode(
      name = nodeName,
      nodeType = elkNode.nodeType,
      nodeQualifier = qualifier,
      // 2. Extract the coordinates ELK calculated
      x = elkNode.node.x.getOrElse(0.0),
      y = elkNode.node.y.getOrElse(0.0),
      width = elkNode.node.width.getOrElse(0.0),
      height = elkNode.node.height.getOrElse(0.0),
      // Recursively map over all children provided by ELK and convert them to DiagramNodes
      children = elkNode.children.map(buildTree(_, allAstNodes, ranges)).toList,
      // Extract the edges directly from the raw ELK node, NOT the Scala wrapper
      edges = elkNode.node.edges.toOption match {
        case Some(jsArray) => jsArray.toList.map(e => toDiagramEdge(e.asInstanceOf[ElkEdgeWithAbs]))
        case None => List.empty[DiagramEdge]
      },
      id = elkNode.id,
      range = ranges.get(elkNode.id)
    )
  }

  // The renderer only reads an edge's id and its routed sections, so that is all the wire carries
  private def toDiagramEdge(edge: ElkEdgeWithAbs): DiagramEdge =
    DiagramEdge(
      id = edge.id.toOption.getOrElse(""),
      sections = edge.sections.toOption.map(_.toList).getOrElse(Nil).map { section =>
        EdgeSection(
          startPoint = toPoint(section.startPoint),
          endPoint = toPoint(section.endPoint),
          bendPoints = section.bendPoints.toOption.map(_.toList).getOrElse(Nil).map(toPoint)
        )
      }
    )

  private def toPoint(p: ElkPoint): Point = Point(p.x, p.y)
}