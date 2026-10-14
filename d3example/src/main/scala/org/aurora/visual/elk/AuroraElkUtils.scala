package org.aurora.visual.elk

import org.aurora.sjsast.PCM
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import typings.elkjs.libElkApiMod.ElkNode
import org.aurora.sjsast.IssueCoordinate
import org.aurora.sjsast.OrderCoordinate
import org.aurora.sjsast.NL_STATEMENT
import typings.elkjs.libElkApiMod.ElkExtendedEdge
import org.aurora.sjsast.AstNode
import org.aurora.sjsast.LHSet
import org.aurora.sjsast.utils.AstNode
import org.aurora.sjsast.{NGO, Orders}

object AuroraElkUtils {  

  // Named groups of orders (NGOs), e.g. "Diet" or "Daily_Labs", in document order
  def namedGroups(pcm: PCM): List[NGO] =
    pcm.cio.values.toList.collect { case o: Orders => o.ngo.toList }.flatten

  def namedGroupNames(pcm: PCM): List[String] =
    namedGroups(pcm).map(_.name).distinct

  // ELK ids of the nodes drawn only for the given named groups: their order coordinates and narratives.
  // A node that is also drawn for something still shown (e.g. the same order in another group) is kept.
  def namedGroupNodeIds(pcm: PCM, groups: Set[String]): Set[String] =
    if (groups.isEmpty) Set.empty
    else {
      def drawableIds(nodes: List[org.aurora.sjsast.AstNode]): Set[String] =
        nodes
          .flatMap(n => n :: AstNode.getAllDescendants(n).toList)
          .collect { case d @ (_: IssueCoordinate | _: OrderCoordinate | _: NL_STATEMENT) => AstNode.getName(d) }
          .toSet

      val (hidden, shown) = namedGroups(pcm).partition(ngo => groups.contains(ngo.name))
      val outsideHiddenGroups = pcm.cio.values.toList.flatMap {
        case o: Orders => o.narratives.toList
        case other     => List(other)
      } ++ shown

      drawableIds(hidden) -- drawableIds(outsideHiddenGroups)
    }

  def getDrawableDescendants(node: AstNode): LHSet[ElkNode] =
    val children = AstNode.getAllDescendants(node)
    children.flatMap(c => {
      c match
        case ic: IssueCoordinate => Option(ic.transformToElkNode)
        case oc: OrderCoordinate => Option(oc.transformToElkNode)
        case n: NL_STATEMENT => Option(n.transformToElkNode)  
        case _ => None           
      })

  def getDrawableEdges(node: AstNode): LHSet[ElkExtendedEdge] =
    val nodeAndChildren = AstNode.getAllDescendants(node).addOne(node)
    nodeAndChildren.flatMap(c => 
      /* for layouting purposes, each edge must have one source and one target */
      val source = AstNode.getName(c)
      val targetNodes = AstNode.getAllEdges(c)
      
      targetNodes.map(targetNode => {
        val targetName = AstNode.getName(targetNode)
        val qualifier = AstNode.getQualifier(targetNode)
        
        // Inject the strictly typed edge class into the ELK edge ID
        ElkExtendedEdge(
          id = s"${source}->${targetName}%%${qualifier.elkType}", // D3 will receive something like this OC1->IC1%%DraftEdge
          sources = js.Array(source), 
          targets = js.Array(targetName)
        )
      })       
      )

}
