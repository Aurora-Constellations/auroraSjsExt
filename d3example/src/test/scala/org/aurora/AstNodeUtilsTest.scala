
import org.scalatest._

import wordspec._
import matchers._
import org.aurora.sjsast.*
import org.aurora.AstNodeUtils.{getAllDescendants, getName}
import org.aurora.TestUtils
import org.aurora.AstNodeUtils.getAllEdges

//discussion about testing workflows, pros/cons 


class AstNodeUtilsTest extends wordspec.AsyncWordSpec with should.Matchers{
 
  "getAllDescendants" should {
    "return many descendants" in {
      val allDescendants = getAllDescendants(TestUtils.pcm)
      allDescendants should contain allElementsOf(TestUtils.allDescendants)

      val issuesDescendants = getAllDescendants(TestUtils.issues)
      issuesDescendants should contain allElementsOf(TestUtils.issuesDescendants)

      val ordersDescendants = getAllDescendants(TestUtils.orders)
      ordersDescendants should contain allElementsOf(TestUtils.ordersDescendants)
    }
    "return one descendant" in {
      val allOCDescendants = getAllDescendants(TestUtils.oc1)
      allOCDescendants should contain only(TestUtils.nar1)
    }
    "return no descendants" in {
      val ngoDescendants = getAllDescendants(TestUtils.ngo2)
      ngoDescendants.isEmpty should be (true)

      val icDescendants = getAllDescendants(TestUtils.ic1)
      icDescendants.isEmpty should be (true)

      val oc3Descendants = getAllDescendants(TestUtils.oc3)
      oc3Descendants.isEmpty should be (true)
    }
  }
  "getAllEdges" should {
    "return many edges" in {
      val oc2Edges = getAllEdges(TestUtils.oc2)
      oc2Edges should contain allElementsOf(TestUtils.oc2Edges)
    }
    "return one edge" in {
      val oc1Edges = getAllEdges(TestUtils.oc1)
      oc1Edges should contain only (TestUtils.nar1)
    }
    "return no edges" in {
      val oc3Edges = getAllEdges(TestUtils.oc3)
      oc3Edges.isEmpty should be (true)
    }
  }
}
