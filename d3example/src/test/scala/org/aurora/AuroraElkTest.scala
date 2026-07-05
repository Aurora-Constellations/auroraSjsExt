
import org.scalatest._

import wordspec._
import matchers._
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import org.aurora.AuroraElkUtils.getDrawableDescendants
import org.aurora.TestUtils
import org.aurora.AuroraElkUtils.getDrawableEdges
import typings.elkjs.libElkApiMod.ElkNode
import org.aurora.AuroraElk
import org.aurora.AstNodeUtils.getAllDescendants
import org.aurora.sjsast.LHMap
import typings.langium.grammarMod
import org.aurora.AstNodeUtils.getName
import scala.scalajs.concurrent.JSExecutionContext
import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success



class AuroraElkTest extends wordspec.AsyncWordSpec with should.Matchers{
  override implicit def executionContext: ExecutionContext = JSExecutionContext.queue
  "AuroraElk" should {
    "create a node with no children and no edges" in {
      val elkNode = ElkNode(id="OC1")
      val auroraElkNode = AuroraElk.Node(elkNode)
      auroraElkNode.children.isEmpty should be (true)
      auroraElkNode.edges.isEmpty should be (true)
    }
    "create a node with one child and no edges" in {
      val elkNode = ElkNode(id="OC1")
      val elkNodeChild = ElkNode(id="nar1")
      elkNode.setChildren(js.Array(elkNodeChild))
      val auroraElkNode = AuroraElk.Node(elkNode)

      auroraElkNode.children.isEmpty should not be (true)
      auroraElkNode.children.head should be (AuroraElk.Node(elkNodeChild))
      auroraElkNode.edges.isEmpty should be (true)
    }
    "create a node with multiple children and no edges" in {
      val elkNode = ElkNode(id="OC1")
      val elkNodeChild1 = ElkNode(id="nar1")
      val elkNodeChild2 = ElkNode(id="nar2")
      elkNode.setChildren(js.Array(elkNodeChild1, elkNodeChild2))
      val auroraElkNode = AuroraElk.Node(elkNode)
      
      auroraElkNode.children.isEmpty should not be (true)
      auroraElkNode.children.head should be (AuroraElk.Node(elkNodeChild1))
      auroraElkNode.children.last should be (AuroraElk.Node(elkNodeChild2))
      auroraElkNode.edges.isEmpty should be (true)
    }
    "create a node with one child and one edge" in {
      val elkNode = ElkNode(id=TestUtils.oc1.name)
      val elkNodeChild = ElkNode(id="nar1")
      elkNode.setChildren(js.Array(elkNodeChild))
      elkNode.setEdges(getDrawableEdges(TestUtils.oc1).toJSArray)
      val auroraElkNode = AuroraElk.Node(elkNode)
      
      auroraElkNode.children.isEmpty should not be (true)
      auroraElkNode.children.head should be (AuroraElk.Node(elkNodeChild))
      auroraElkNode.edges.isEmpty should not be (true)
    }
    "create a node with multiple children and multiple edges" in {
      val elkNode = ElkNode(id=TestUtils.oc4.name)
      val elkNodeChild1 = ElkNode(id="nar1")
      val elkNodeChild2 = ElkNode(id="nar2")

      elkNode.setChildren(js.Array(elkNodeChild1, elkNodeChild2))
      elkNode.setEdges(getDrawableEdges(TestUtils.oc4).toJSArray)
      val auroraElkNode = AuroraElk.Node(elkNode)
      
      auroraElkNode.children.isEmpty should not be (true)
      auroraElkNode.children.head should be (AuroraElk.Node(elkNodeChild1))
      auroraElkNode.children.last should be (AuroraElk.Node(elkNodeChild2))
      auroraElkNode.edges.isEmpty should not be (true)
    }
    "create a graph" in {
      val layoutOptions = LHMap("elk.algorithm" -> "org.eclipse.elk.layered",
                                "elk.direction" -> "left")
      val elkRoot = AuroraElk.Graph(TestUtils.pcm, layoutOptions)
      val elkRootNode = elkRoot.graph

      for
        descendants <- elkRootNode.map(n => n.children)
      yield
        val names = descendants.map(d => d.id).toList
        val cleanedNames = names.map(n => n.split("%%").headOption.getOrElse("Unknown"))
        cleanedNames should contain allElementsOf {
          TestUtils.allDrawableDescendants.map(getName).map(n => n.split("%%").head)
        }

      for
        nodes <- elkRootNode.map(n => n.children)
      yield
        val xCoords = nodes.map(n => n.xCoord).toList
        xCoords should not contain (0)
        val yCoords = nodes.map(n => n.yCoord).toList
        yCoords should not contain (0)       

    }
  }

}
