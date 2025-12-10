package org.aurora.sjsast.yash

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.aurora.sjsast.catsgivens.given
import cats.syntax.semigroup._
import org.aurora.sjsast.* 

class YashIdempotencyTest extends AnyWordSpec with Matchers:

  "PCM merge" should {
    "be idempotent" in {
      val ref = QuReference("", "chf")
      val oc = OrderCoordinate("NAS", Set("test"), QuReferences(Set(ref)))
      val ngo = NGO("Diet", Set(oc), Set.empty, QuReferences(Set.empty), Set.empty)
      val orders = Orders(Set(ngo), Set.empty)
      val pcm = PCM(Map("Orders" -> orders))

      // Idempotency: a |+| a == a
      (pcm |+| pcm) shouldBe pcm
    }

    "merge same NGO only once" in {
      val ref1 = QuReference("", "chf")
      val ref2 = QuReference("", "mi")
      
      val oc1 = OrderCoordinate("NAS", Set.empty, QuReferences(Set(ref1)))
      val oc2 = OrderCoordinate("NAS", Set.empty, QuReferences(Set(ref2)))
      
      val ngo1 = NGO("Diet", Set(oc1), Set.empty, QuReferences(Set.empty), Set.empty)
      val ngo2 = NGO("Diet", Set(oc2), Set.empty, QuReferences(Set.empty), Set.empty)
      
      val orders1 = Orders(Set(ngo1), Set.empty)
      val orders2 = Orders(Set(ngo2), Set.empty)
      
      val pcm1 = PCM(Map("Orders" -> orders1))
      val pcm2 = PCM(Map("Orders" -> orders2))

      val merged = pcm1 |+| pcm2
      val mergedAgain = merged |+| pcm1 |+| pcm2

      // Should be same after merging again
      merged shouldBe mergedAgain
    }
  }

end YashIdempotencyTest