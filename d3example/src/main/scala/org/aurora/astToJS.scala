package org.aurora

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

// 1. Define the strictly typed Scala case class
case class D3Node(
  name: String, 
  nodeType: String, 
  x: Double, 
  y: Double, 
  width: Double, 
  height: Double, 
  children: List[D3Node] = List.empty,
  edges: List[js.Any] = List.empty
) {
  
  // 2. Helper to convert the Scala class into a plain JavaScript object for D3
  def toJS: js.Any = {
    js.Dictionary(
      "name" -> name,
      "nodeType" -> nodeType,
      "x" -> x,
      "y" -> y,
      "width" -> width,
      "height" -> height,
      "children" -> children.map(_.toJS).toJSArray,
      "edges" -> edges.toJSArray
    )
  }
}

object AstTransformer {
  
  def fromElkToD3Node(elkNode: AuroraElk.Node): D3Node = {
    // In AuroraElk.scala, we see the ID often contains "%%"
    // We can extract the raw name by taking the first part of that ID.
    val nodeName = elkNode.id.split("%%").headOption.getOrElse("Unknown")
    
    D3Node(
      name = nodeName,
      nodeType = elkNode.nodeType,
      // 2. Extract the coordinates ELK calculated
      x = elkNode.node.x.getOrElse(0.0),
      y = elkNode.node.y.getOrElse(0.0),
      width = elkNode.node.width.getOrElse(0.0),
      height = elkNode.node.height.getOrElse(0.0),
      // Recursively map over all children provided by ELK and convert them to D3Nodes
      children = elkNode.children.map(child => fromElkToD3Node(child)).toList,
      // Extract the edges directly from the raw ELK node, NOT the Scala wrapper
      edges = elkNode.node.edges.toOption match {
        case Some(jsArray) => jsArray.toList.map(_.asInstanceOf[js.Any])
        case None => List.empty[js.Any]
      }
    )
  }
}