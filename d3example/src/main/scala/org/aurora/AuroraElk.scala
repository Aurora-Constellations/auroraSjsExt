package org.aurora

import org.aurora.sjsast.PCM
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
import org.aurora.sjsast.LHSet
import org.aurora.sjsast.LHMap
import org.scalablytyped.runtime.StringDictionary

object AuroraElk:

    case class Node(node: ElkNode):
        val id: String = node.id_ElkNode

        val children: LHSet[Node] = node.children match {
            case array: js.Array[ElkNode] => LHSet.from(array.iterator.map(Node.apply))
            case _: Unit => LHSet.empty[Node]
        }
        val edges: LHSet[Edge] = node.edges match {
            case array: js.Array[ElkExtendedEdge] => LHSet.from(array.iterator.map(Edge.apply))
            case _: Unit => LHSet.empty[Edge] 
        }
        val layout: String = node.layoutOptions.toOption.flatMap(opts => opts.get("elk.algorithm")).getOrElse("FORCE") /* default */

        override def toString(): String = 
            s"Node(id=${id}, children=${children.size})"

        override def equals(that: Any): Boolean = 
            that match {
                case n: Node => 
                    n.id == this.id && n.children == this.children && n.edges == this.edges
                case _ => false
            }

    case class Edge(edge: ElkExtendedEdge):
        val id: String = edge.id match {
            case s: String => s
            case _ => "<no_id>"
        }
        val sources: LHSet[String] = LHSet.from(edge.sources)
        val targets: LHSet[String] = LHSet.from(edge.targets)

    case class Graph(pcm: PCM, layoutOptions: LHMap[String, String]): 
        val elkRoot: ElkNode = ElkNode(id="root")
        val layoutOptionsDictionary = layoutOptions.toJSDictionary.asInstanceOf[LayoutOptions]
        
        elkRoot.setChildren(getDrawableDescendants(pcm).toJSArray)
        elkRoot.setEdges(getDrawableEdges(pcm).toJSArray)        
        elkRoot.setLayoutOptions(layoutOptionsDictionary)
        
        val graph = Node(elkRoot)
        lazy val children = graph.children
        lazy val edges = graph.edges
        lazy val layout = graph.layout



    


