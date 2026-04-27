package org.aurora

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import org.aurora.sjsast.*
import typings.elkjs.libElkApiMod.{ElkNode, ElkEdge}
import typings.elkjs.elkjsStrings.children

object AstTransformer:
  def toD3(node: Any): js.Dynamic =
    node match
      case p: PCM =>
        js.Dynamic.literal(
          name = "PCM",
          nodeType = "Root",
          children = p.cio.values.map(toD3).toJSArray
        )
      case o: Orders =>
        js.Dynamic.literal(
          name = "Orders",
          nodeType = "Module",
          children = o.ngo.map(toD3).toJSArray
        )
      case n: NGO =>
        js.Dynamic.literal(
          name = s"NGO: ${n.name}",
          nodeType = "Node",
          children = n.ordercoord.map(toD3).toJSArray
        )
      case oc: OrderCoordinate =>
        js.Dynamic.literal(
          name = s"Coord: ${oc.name}",
          nodeType = "Coordinate",
          children = (oc.narratives.map(toD3) ++ oc.qurefs.map(toD3)).toJSArray
        )
      case nl: NL_STATEMENT =>
        js.Dynamic.literal(
          name = s"NL: ${nl.name}",
          nodeType = "Statement",
          children = js.Array()
        )
      case qrs: QuReferences =>
        js.Dynamic.literal(
          name = "QuReferences",
          nodeType = "Collection",
          children = qrs.qurc.map(toD3).toJSArray
        )
      case qr: QuReference =>
        js.Dynamic.literal(
          name = s"Ref: ${qr.refName}",
          nodeType = "Reference",
          children = js.Array(toD3(qr.qu))
        )
      case q: QU =>
        js.Dynamic.literal(
          name = "QU",
          nodeType = "Query",
          children = js.Array()
        )
      case other =>
        js.Dynamic.literal(
          name = other.toString,
          nodeType = "Unknown",
          children = js.Array()
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

