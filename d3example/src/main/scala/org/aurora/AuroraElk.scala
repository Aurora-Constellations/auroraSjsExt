package org.aurora

import org.aurora.sjsast.PCM
import org.aurora.AuroraElkUtils.AuroraElkParameters
import typings.elkjs.libElkApiMod.{ElkNode}
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import typings.elkjs.libElkApiMod.ElkExtendedEdge
import org.aurora.sjsast.Issues
import org.aurora.sjsast.Orders
import org.aurora.sjsast.IssueCoordinate
import org.aurora.sjsast.OrderCoordinate
import typings.elkjs.libElkApiMod.LayoutOptions
import org.aurora.sjsast.AstNode
import org.aurora.sjsast.NL_STATEMENT
import org.aurora.sjsast.QuReference
import org.aurora.AuroraElkUtils.*

/* Add QU children for relevant */

object AuroraElk:

    case class AuroraElkNode(node: ElkNode):
        val children: js.Array[AuroraElkNode] = node.children.getOrElse(JsArrayUtils.empty[ElkNode]).map(c => AuroraElkNode(c))
        val edges: js.Array[AuroraElkEdge] = node.edges.getOrElse(JsArrayUtils.empty[ElkExtendedEdge]).map(e => AuroraElkEdge(e))
        val layout: String = node.layoutOptions.toOption.flatMap(opts => opts.get("elk.algorithm")).getOrElse("FORCE") /* default */

    case class AuroraElkEdge(edge: ElkExtendedEdge):
        val sources: js.Array[String] = edge.sources
        val targets: js.Array[String] = edge.targets

    case class AuroraElkGraph(pcm: PCM, graphParams: AuroraElkParameters):
        val elkJsGraphObject: ElkNode = js.Dynamic.literal(
                                                id = "root",
                                                layoutOptions = js.Dynamic.literal(
                                                    "elk.algorithm" -> graphParams.layout.toString(),
                                                    "elk.direction" -> graphParams.direction.toString()
                                                ),
                                                children = getDrawableChildren(pcm),
                                                edges = getDrawableEdges(pcm)
                                            ).asInstanceOf[ElkNode]
        
        lazy val graph = AuroraElkNode(elkJsGraphObject)
        lazy val children = graph.children
        lazy val edges = graph.edges
        lazy val layout = graph.layout



    


