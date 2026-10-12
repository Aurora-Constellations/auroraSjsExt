package org.aurora.visual.d3

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import org.aurora.sjsast.*
import org.aurora.sjsast.utils.*
import typings.vscodeLanguageserverProtocol.libCommonProtocolMod._InitializeParams
import org.aurora.visual.elk.AuroraElk

// 1. Define the strictly typed Scala case class
case class D3Node(
  name: String, 
  nodeType: String, 
  nodeQualifier: String,
  x: Double, 
  y: Double, 
  width: Double, 
  height: Double, 
  children: List[D3Node] = List.empty,
  edges: List[js.Any] = List.empty,
  id: String = "",
  range: Option[SourceRange] = None
) {
  
  // 2. Helper to convert the Scala class into a plain JavaScript object for D3
  def toJS: js.Any = {
    js.Dictionary(
      "name" -> name,
      "nodeType" -> nodeType,
      "nodeQualifier" -> nodeQualifier,
      "x" -> x,
      "y" -> y,
      "width" -> width,
      "height" -> height,
      "children" -> children.map(_.toJS).toJSArray,
      "edges" -> edges.toJSArray,
      "id" -> id,
      "range" -> range.map(_.toJS).getOrElse(js.undefined)
    )
  }
}

object AstTransformer {
  
  def fromElkToD3Node(
    elkNode: AuroraElk.Node,
    pcm: PCM,
    ranges: Map[String, SourceRange] = Map.empty
  ): D3Node = {
    val allAstNodes = AstNode.getAllDescendants(pcm).toList
    buildTree(elkNode, allAstNodes, ranges)
  }

  private def buildTree(
    elkNode: AuroraElk.Node,
    allAstNodes: List[AstNode],
    ranges: Map[String, SourceRange]
  ): D3Node = {
    
    // In AuroraElk.scala, we see the ID often contains "%%"
    // We can extract the raw name by taking the first part of that ID.
    val nodeName = elkNode.id.split("%%").headOption.getOrElse("Unknown")
    val astNode = allAstNodes.find {
      case ic: IssueCoordinate => elkNode.nodeType == "Reference" && ic.name == nodeName
      case oc: OrderCoordinate => elkNode.nodeType == "Coordinate" && oc.name == nodeName
      case _ => false
    }
    val qualifier = astNode.map(AstNode.getQualifier).getOrElse(Qualifier.Normal).elkType
    
    D3Node(
      name = nodeName,
      nodeType = elkNode.nodeType,
      nodeQualifier = qualifier,
      // 2. Extract the coordinates ELK calculated
      x = elkNode.node.x.getOrElse(0.0),
      y = elkNode.node.y.getOrElse(0.0),
      width = elkNode.node.width.getOrElse(0.0),
      height = elkNode.node.height.getOrElse(0.0),
      // Recursively map over all children provided by ELK and convert them to D3Nodes
      children = elkNode.children.map(buildTree(_, allAstNodes, ranges)).toList,
      // Extract the edges directly from the raw ELK node, NOT the Scala wrapper
      edges = elkNode.node.edges.toOption match {
        case Some(jsArray) => jsArray.toList.map(_.asInstanceOf[js.Any])
        case None => List.empty[js.Any]
      },
      id = elkNode.id,
      range = ranges.get(elkNode.id)
    )
  }
}