import org.scalatest._

import wordspec._
import matchers._
import scala.scalajs.js
import org.aurora.visual.d3.{Arrowheads, ElkEdgeWithAbs}

class ArrowheadsTest extends wordspec.AnyWordSpec with should.Matchers {
  private def edge(id: String, source: String, target: String): ElkEdgeWithAbs =
    js.Dynamic.literal(id = id, sources = js.Array(source), targets = js.Array(target)).asInstanceOf[ElkEdgeWithAbs]

  private val orderToIssue = edge("OC1%%Coordinate->IC1%%Reference%%Urgent", "OC1%%Coordinate", "IC1%%Reference")
  private val orderToNarrative = edge("OC1%%Coordinate->?? daily;%%DraftNarrative%%Normal", "OC1%%Coordinate", "?? daily;%%DraftNarrative")
  private val issueToNarrative = edge("IC1%%Reference->-- note;%%NormalNarrative%%Normal", "IC1%%Reference", "-- note;%%NormalNarrative")

  "Arrowheads.pointsToSource" should {
    "point order -> issue edges back at the order (Issue -> Order)" in {
      Arrowheads.pointsToSource(orderToIssue) should be (true)
    }
    "point at the narrative from its order or issue" in {
      Arrowheads.pointsToSource(orderToNarrative) should be (false)
      Arrowheads.pointsToSource(issueToNarrative) should be (false)
    }
  }

  "Arrowheads.qualifierOf" should {
    "read the qualifier from the end of the edge id" in {
      Arrowheads.qualifierOf(orderToIssue) should be ("Urgent")
      Arrowheads.qualifierOf(orderToNarrative) should be ("Normal")
    }
    "fall back to Normal when the id carries no qualifier" in {
      Arrowheads.qualifierOf(edge("plain", "a", "b")) should be ("Normal")
    }
  }
}
