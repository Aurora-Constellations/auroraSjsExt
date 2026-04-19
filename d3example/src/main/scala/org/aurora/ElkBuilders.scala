package org.aurora

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import org.aurora.sjsast.*
import typings.elkjs.mod.default as ELK
import typings.elkjs.libElkApiMod.{ElkNode, ElkEdge}

object ElkBuilders {

  def node(
    id: String,
    width: Double,
    height: Double,
    children: Seq[ElkNode] = Seq.empty,
    edges: Seq[ElkEdge] = Seq.empty,
    layoutOptions: Map[String, String] = Map.empty
  ): ElkNode =
    js.Dynamic.literal(
      id = id,
      width = width,
      height = height,
      children = children.toJSArray,
      edges = edges.toJSArray,
      layoutOptions = js.Dictionary(layoutOptions.toSeq*)
    ).asInstanceOf[ElkNode]

  def edge(id: String, source: String, target: String): ElkEdge =
    js.Dynamic.literal(
      id = id,
      sources = js.Array(source),
      targets = js.Array(target)
    ).asInstanceOf[ElkEdge]
}