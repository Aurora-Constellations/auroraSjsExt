package org.aurora

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import org.aurora.sjsast.*

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