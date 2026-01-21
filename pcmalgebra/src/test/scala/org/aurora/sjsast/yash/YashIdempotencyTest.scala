package org.aurora.sjsast.yash

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.aurora.sjsast.JoinMeet.*
import org.aurora.sjsast.* 
import scala.collection.mutable.LinkedHashMap

class YashIdempotencyTest extends AnyWordSpec with Matchers:

  "PCM merge" should {
    "be idempotent" in {
      val ref = QuReference(QU(LHSet('~')), "chf")
      val oc = OrderCoordinate("NAS", LHSet(NL_STATEMENT("test")), QuReferences(LHSet(ref)))
      val ngo = NGO(name="Diet", ordercoord=LHSet(oc), narratives = LHSet(), qurefs=QuReferences(LHSet()), qu=LHSet())
      val orders = Orders(ngo=LHSet(ngo), narratives = LHSet())
      val pcm = PCM("",LinkedHashMap("Orders" -> orders))

      // Idempotency: a |+| a == a
      (pcm |+| pcm) shouldBe pcm
    }

    "merge same NGO only once" in {
      val ref1 = QuReference(QU(LHSet('~')), "chf")
      val ref2 = QuReference(QU(LHSet('~')), "mi")

      val oc1 = OrderCoordinate("NAS", LHSet(), QuReferences(LHSet(ref1)))
      val oc2 = OrderCoordinate("NAS", LHSet(), QuReferences(LHSet(ref2)))

      val ngo1 = NGO(name="Diet", ordercoord=LHSet(oc1), narratives = LHSet(), qurefs=QuReferences(LHSet()), qu=LHSet())
      val ngo2 = NGO(name="Diet", ordercoord=LHSet(oc2), narratives = LHSet(), qurefs=QuReferences(LHSet()), qu=LHSet())

      val orders1 = Orders(ngo=LHSet(ngo1), narratives = LHSet())
      val orders2 = Orders(ngo=LHSet(ngo2), narratives = LHSet())

      val pcm1 = PCM("",LinkedHashMap("Orders" -> orders1))
      val pcm2 = PCM("",LinkedHashMap("Orders" -> orders2))
      val merged = pcm1 |+| pcm2
      val mergedAgain = merged |+| pcm1 |+| pcm2

      // Should be same after merging again
      merged shouldBe mergedAgain
    }
  }

end YashIdempotencyTest