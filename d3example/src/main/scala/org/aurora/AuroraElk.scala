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
import typings.elkjs.mod.default as ELKConstructor
import scala.concurrent.Future
import scala.scalajs.js.Thenable.Implicits._
import scala.concurrent.ExecutionContext

object AuroraElk:

    // SUNDAY TO DO: ADD NODE TYPES, idea: add to elk node name and filter by clean name later in auroraelk.node?

    case class Node(node: ElkNode):
        val id = node.id_ElkNode
        lazy val nodeType = id.split("%%").lastOption.getOrElse("Unknown")

        val children: LHSet[Node] = node.children match {
            case array: js.Array[ElkNode] => LHSet.from(array.iterator.map(Node.apply))
            case _: Unit => LHSet.empty[Node]
        }
        val edges: LHSet[Edge] = node.edges match {
            case array: js.Array[ElkExtendedEdge] => LHSet.from(array.iterator.map(Edge.apply))
            case _: Unit => LHSet.empty[Edge] 
        }
        val layout: String = node.layoutOptions.toOption.flatMap(opts => opts.get("elk.algorithm"))
                                                        .getOrElse("org.eclipse.elk.layered") /* default */

        val xCoord: Double = node.x.toOption.getOrElse(0)

        val yCoord: Double = node.y.toOption.getOrElse(0)

        override def toString(): String = 
            /* we can change this later if we want to expose different info */
            s"Node(id=${id}, children=${children.size})" 

        override def equals(that: Any): Boolean = 
            that match {
                case n: Node => n.id == this.id && n.children == this.children && n.edges == this.edges
                case _ => false
            }

    case class Edge(edge: ElkExtendedEdge):
        val id: String = edge.id match {
            case s: String => s
            case _ => "<no_id>"
        }
        val sources: LHSet[String] = LHSet.from(edge.sources)
        val targets: LHSet[String] = LHSet.from(edge.targets)

    case class Graph(pcm: PCM, layoutOptions: LHMap[String, String]) :
        implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
        private val elk = new ELKConstructor()
        private val elkRoot: ElkNode = ElkNode(id="root")
        private val layoutOptionsDictionary = layoutOptions.toJSDictionary.asInstanceOf[LayoutOptions]      
        
        elkRoot.setChildren(getDrawableDescendants(pcm).toJSArray)
        elkRoot.setEdges(getDrawableEdges(pcm).toJSArray)        
        elkRoot.setLayoutOptions(layoutOptionsDictionary)

        lazy val graph = runLayout()

        def runLayout(): Future[Node] = for {
                                        laidOutRoot <- elk.layout(elkRoot).toFuture.recover { 
                                            case e =>
                                                println("ELK layout failed:")
                                                println(e.getMessage)
                                                e.printStackTrace()

                                                throw e
                                            }
                                        } yield {
                                            println("ELK layout succeeded:")
                                            println(laidOutRoot)
                                            Node(laidOutRoot)
                                        }
                        
                    



    


