
import org.scalatest._

import wordspec._
import matchers._
import org.aurora.visual.elk.AuroraElkUtils.getDrawableDescendants
import org.aurora.TestUtils
import org.aurora.visual.elk.AuroraElkUtils.getDrawableEdges
import org.aurora.visual.elk.AuroraElkUtils.{namedGroupNames, namedGroupNodeIds}
import org.aurora.sjsast.*


class AuroraElkUtilsTest extends wordspec.AsyncWordSpec with should.Matchers{
  "getDrawableChildren" should {
    "return many children" in {
      val allDrawableChildren = getDrawableDescendants(TestUtils.pcm)
      allDrawableChildren.size should be (TestUtils.allDrawableDescendants.size)

      val drawableIssuesDescendants = getDrawableDescendants(TestUtils.issues)
      drawableIssuesDescendants.size should be (TestUtils.drawableIssuesDescendants.size)

      val drawableOrdersDescendants = getDrawableDescendants(TestUtils.orders)
      drawableOrdersDescendants.size should be (TestUtils.drawableOrdersDescendants.size)
    }
    "return one child" in {
      val drawableOc1Descendants = getDrawableDescendants(TestUtils.oc1)
      drawableOc1Descendants.size should be (TestUtils.drawableOc1Descendants.size)
    }
    "return no children" in {
      val drawableNar1Descendants = getDrawableDescendants(TestUtils.nar1)
      drawableNar1Descendants.isEmpty should be (true)

      val drawableOc3Descendants = getDrawableDescendants(TestUtils.oc3)
      drawableOc3Descendants.isEmpty should be (true)

      val drawableNgo2Descendants = getDrawableDescendants(TestUtils.ngo2)
      drawableNgo2Descendants.isEmpty should be (true)
    }
  }
  "getDrawableEdges" should {
    "return many edges" in {
      val allDrawableEdges = getDrawableEdges(TestUtils.pcm)
      allDrawableEdges.size should be (TestUtils.allDrawableEdges.size)

      val drawableNgo1Edges = getDrawableEdges(TestUtils.ngo1)
      drawableNgo1Edges.size should be (TestUtils.drawableNgo1Edges.size)
    }
    "return one edge" in {
      val drawableOc1Edges = getDrawableEdges(TestUtils.oc1)
      drawableOc1Edges.size should be (TestUtils.drawableOc1Edges.size)
    }
    "return no edges" in {
      val drawableNar1Edges = getDrawableEdges(TestUtils.nar1)
      drawableNar1Edges.isEmpty should be (true)

      val drawableNar2Edges = getDrawableEdges(TestUtils.nar2)
      drawableNar2Edges.isEmpty should be (true)

      val drawableOc3Edges = getDrawableEdges(TestUtils.oc3)
      drawableOc3Edges.isEmpty should be (true)

    }
  }
  "namedGroupNames" should {
    "list the named groups of orders" in {
      namedGroupNames(TestUtils.pcm) should be (List("NGO1", "NGO2"))
    }
  }
  "namedGroupNodeIds" should {
    "return the orders and narratives of the hidden groups" in {
      namedGroupNodeIds(TestUtils.pcm, Set("NGO1")) should be (Set(
        "OC1%%Coordinate", "OC2%%Coordinate", "OC3%%Coordinate",
        "nar1%%NormalNarrative", "nar2%%NormalNarrative"
      ))
    }
    "return nothing when no group is hidden" in {
      namedGroupNodeIds(TestUtils.pcm, Set.empty).isEmpty should be (true)
      namedGroupNodeIds(TestUtils.pcm, Set("NGO2")).isEmpty should be (true)
    }
    "keep an order that also belongs to a group still shown" in {
      val groupA = NGO(name = "A", ordercoord = LHSet(TestUtils.oc1, TestUtils.oc3))
      val groupB = NGO(name = "B", ordercoord = LHSet(TestUtils.oc1))
      val pcm = PCM(LHMap("Orders" -> Orders(ngo = LHSet(groupA, groupB))))

      namedGroupNodeIds(pcm, Set("A")) should be (Set("OC3%%Coordinate"))
    }
  }

}
