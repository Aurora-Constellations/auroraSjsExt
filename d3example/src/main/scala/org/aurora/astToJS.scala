package org.aurora

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import org.aurora.sjsast.*

// 1. Define the strictly typed Scala case class
case class D3Node(name: String, nodeType: String, children: List[D3Node] = List.empty) {
  
  // 2. Helper to convert the Scala class into a plain JavaScript object for D3
  def toJS: js.Any = {
    js.Dictionary(
      "name" -> name,
      "nodeType" -> nodeType,
      "children" -> children.map(_.toJS).toJSArray
    )
  }
}
import typings.elkjs.libElkApiMod.{ElkNode, ElkEdge, ElkExtendedEdge}
import typings.elkjs.elkjsStrings.children

object AstTransformer:
  
  // 3. Return the case class instead of js.Dynamic
  def toD3Node(node: AstNode): D3Node =
    node match
      case p: PCM =>
        D3Node(
          name = "PCM",
          nodeType = "Unknown",
          children = p.cio.values.map(item => toD3Node(item.asInstanceOf[AstNode])).toList
        )
      case o: Orders =>
        D3Node(
          name = "Orders",
          nodeType = "Unknown",
          children = o.ngo.map(item => toD3Node(item.asInstanceOf[AstNode])).toList
        )
      case n: NGO =>
        D3Node(
          name = n.name,
          nodeType = "Unknown",
          children = n.ordercoord.map(item => toD3Node(item.asInstanceOf[AstNode])).toList
        )
      case oc: OrderCoordinate =>
        D3Node(
          name = oc.name,
          nodeType = "Coordinate",
          children = (oc.narratives.map(item => toD3Node(item.asInstanceOf[AstNode])) ++ oc.qurefs.map(item => toD3Node(item.asInstanceOf[AstNode]))).toList
        )
      case nl: NL_STATEMENT =>
        D3Node(
          name = nl.name,
          nodeType = "Statement"
        )
      case qrs: QuReferences =>
        D3Node(
          name = "QuReferences",
          nodeType = "Unknown",
          children = qrs.qurc.map(item => toD3Node(item.asInstanceOf[AstNode])).toList
        )
      case qr: QuReference =>
        D3Node(
          name = qr.refName,
          nodeType = "Reference",
        )
      case other =>
        D3Node(
          name = other.toString,
          nodeType = "Unknown"
        )
  
  