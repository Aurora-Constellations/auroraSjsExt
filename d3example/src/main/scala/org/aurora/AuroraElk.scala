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
import org.aurora.PcmUtils.getTopLevelChildren

object AuroraElk:

    case class AuroraElkNode(node: ElkNode):
        val children: js.Array[AuroraElkNode] = node.children.getOrElse(JsArrayUtils.empty[ElkNode]).map(c => AuroraElkNode(c))
        val edges: js.Array[AuroraElkEdge] = node.edges.getOrElse(JsArrayUtils.empty[ElkExtendedEdge]).map(e => AuroraElkEdge(e))

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
                                                children = getAndCreateElkChildren(pcm),
                                                edges = ???
                                            ).asInstanceOf[ElkNode]
        
        lazy val graph = AuroraElkNode(elkJsGraphObject)
        lazy val children = graph.children
        lazy val edges = graph.edges

    private def getAndCreateElkChildren(node: Any): js.Array[Any] =
        PcmUtils.getTopLevelChildren(node).map(child => {
            val name = child._1
            val value = child._2
            val children = getTopLevelChildren(value).map(c => c._2.asInstanceOf[Any]).toJSArray
            createElkNode(name, children)
        }).toJSArray

    private def createElkNode(id: String, children: js.Array[Any]): ElkNode =
        js.Dynamic.literal(id = id, children = children).asInstanceOf[ElkNode]

    private def getAndCreateElkEdges(node: Any): js.Array[Any] = 
        val sourcesAndTargets = PcmUtils.getTargetsFromChildrenAndRefs(node)
        sourcesAndTargets.map((s,t) => {
            createElkExtendedEdge(s.concat(t.join(".")), s, t)
        }).toJSArray
        

    private def createElkExtendedEdge(id: String, source: String, targets: js.Array[String]): ElkExtendedEdge =
        js.Dynamic.literal(id = id, sources = js.Array(source), targets = targets).asInstanceOf[ElkExtendedEdge]

    


