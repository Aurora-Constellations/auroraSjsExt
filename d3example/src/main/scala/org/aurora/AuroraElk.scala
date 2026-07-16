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

    // in the future for offsetting, keep it separate/decoupled from the IR, but we can use it as part of a composite type for offset info

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

        //Create a helper to enforce width and height on every node
        def applyDimensions(nodes: js.Array[ElkNode]): Unit = {
            nodes.foreach { n =>
                // Extract the clean name to measure its length
                val nodeName = n.id.toOption.getOrElse("Unknown").split("%%").headOption.getOrElse("Unknown")
                val calculatedWidth = (nodeName.length * 7) + 20.0

                // Assign the true dynamic width to ELK
                n.width = calculatedWidth
                n.height = 40.0 // We can keep height fixed for standard text

                n.children.toOption.foreach { childArray =>
                applyDimensions(childArray)
                }
            }
        }
        
        // 2. Fetch the children, apply the sizes, and pass them to ELK
        val rawChildren = getDrawableDescendants(pcm).toJSArray
        applyDimensions(rawChildren)     
        
        elkRoot.setChildren(rawChildren)
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
                                        } // to do: add this to a future logging infrastructure
                        
                    



    


