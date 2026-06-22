package org.aurora
import org.aurora.sjsast.*

object TestUtils {
    /**
    Issues:
      IC1
      IC2
    Orders:
      NGO1:
        OC1 -nar1
        OC2(IC1) -nar2
        OC3
      NGO2:
  **/

  val nar1 = NL_STATEMENT("nar1")
  val nar2 = NL_STATEMENT("nar2")

  val ic1 = IssueCoordinate(name="IC1")
  val ic2 = IssueCoordinate(name="IC2")

  val quRef1 = QuReference(refName = "IC1")
  val quRefs1 = QuReferences(LHSet(quRef1))

  val oc1 = OrderCoordinate(name="OC1", LHSet(nar1))
  val oc2 = OrderCoordinate(name="OC2", LHSet(nar2), qurefs = LHSet(quRefs1))
  val oc3 = OrderCoordinate(name="OC3")

  val ngo1 = NGO(name="NGO1", ordercoord = LHSet(oc1, oc2, oc3))
  val ngo2 = NGO(name="NGO2")

  val issues = Issues(ic=LHSet(ic1, ic2))
  val orders = Orders(ngo=LHSet(ngo1, ngo2))
  val pcm = PCM(LHMap("Issues" -> issues, "Orders" -> orders))

  val allDescendants = List(nar1, nar2, ic1, ic2, oc1, oc2, oc3, ngo1, ngo2)
  val issuesDescendants = List(ic1, ic2)
  val ordersDescendants = List(ngo1, ngo2, oc1, oc2, oc3, nar1, nar2)
  val oc2Edges = List(quRef1, nar2)

  val allDrawableDescendants = List(nar1, nar2, ic1, ic2, oc1, oc2, oc3)
}
