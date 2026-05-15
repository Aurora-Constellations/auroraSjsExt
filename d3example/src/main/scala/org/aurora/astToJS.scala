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
import typings.elkjs.libElkApiMod.{ElkNode, ElkEdge}
import typings.elkjs.elkjsStrings.children

object AstTransformer:
  
  // 3. Return the case class instead of js.Dynamic
  def toD3Node(node: Any): D3Node =
    node match
      case p: PCM =>
        D3Node(
          name = "PCM",
          nodeType = "Unknown",
          children = p.cio.values.map(toD3Node).toList
        )
      case o: Orders =>
        D3Node(
          name = "Orders",
          nodeType = "Unknown",
          children = o.ngo.map(toD3Node).toList
        )
      case n: NGO =>
        D3Node(
          name = n.name,
          nodeType = "Unknown",
          children = n.ordercoord.map(toD3Node).toList
        )
      case oc: OrderCoordinate =>
        D3Node(
          name = oc.name,
          nodeType = "Coordinate",
          children = (oc.narratives.map(toD3Node) ++ oc.qurefs.map(toD3Node)).toList
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
          children = qrs.qurc.map(toD3Node).toList
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
  
  def toElkRoot(pcm: PCM, layoutAlgorithm: String = "LAYERED", layoutDirection: String = "RIGHT"): ElkNode =
    /* PCM => Elk Graph Root Node */
    /* Need a cleaner way to do this 
        - this is just a temporary and incomplete proof-of-concept (will add other grammar elements later)
        -- will probably need a separate function for this, will probably need to traverse the whole tree and get all descendants */
    val children: js.Array[ElkNode] = pcm.cio.get("Orders")
                          .collect{case o: Orders => o.ngo}
                          .map(ngoArray => ngoArray.map(ngo => {
                            val ngoOcChildren = ngo.ordercoord
                            val ngoGrandchildren = ngoOcChildren.flatMap(oc => oc.narratives)
                            val ngoElkGrandchildren = ngoGrandchildren.map(nl => toElkNode(nl.name))
                            val ngoOcElkChildren = ngoOcChildren.map(oc => toElkNode(oc.name, ngoElkGrandchildren))
                            toElkNode(ngo.name, ngoOcElkChildren)
                          })).getOrElse(LHSet.empty).toJSArray
    val edges: js.Array[ElkEdge] = pcm.cio.get("Orders")
                                      .collect{case o: Orders => o.ngo}
                                      .map(ngoArray => ngoArray.flatMap(ngo => {
                                        val ngoOcChildren = ngo.ordercoord
                                        val primaryEdges = ngoOcChildren.map(oc => toElkEdge(ngo.name + "_" + oc.name, ngo.name, oc.name))
                                        val secondaryEdges = ngoOcChildren.flatMap(oc => {
                                          oc.narratives.map(nar => toElkEdge(oc.name + "_" + nar.name, oc.name, nar.name))
                                        })
                                        secondaryEdges.foreach(edge => primaryEdges.add(edge))
                                        primaryEdges
                                      })).getOrElse(LHSet.empty).toJSArray
    js.Dynamic.literal(
      id = "root",
      layoutOptions = js.Dynamic.literal(
        "elk.algorithm" -> layoutAlgorithm,
        "elk.direction" -> layoutDirection
      ),
      children = children,
      edges = edges
    ).asInstanceOf[ElkNode]


  private def toElkNode(id: String, children: LHSet[ElkNode] = LHSet.empty): ElkNode =
    js.Dynamic.literal(id = id, children = children).asInstanceOf[ElkNode]

  private def toElkEdge(id: String, source: String, target: String): ElkEdge =
    js.Dynamic.literal(id = id, sources = js.Array(source), targets = js.Array(target)).asInstanceOf[ElkEdge]

