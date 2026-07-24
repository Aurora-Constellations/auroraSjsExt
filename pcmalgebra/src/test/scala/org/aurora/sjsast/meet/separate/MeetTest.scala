package org.aurora.sjsast.meet.separate

import org.aurora.sjsast.*
import org.aurora.sjsast.meet.separate.Meet.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class MeetTest extends AnyWordSpec with Matchers:

  "Meet" should {
    "intersect basic collections" in {
      (Set(1, 2) |&| Set(2, 3)) shouldBe Set(2)
      summon[Meet[Boolean]].meet(true, false) shouldBe false
      summon[Meet[Set[Int]]].meet(Set(1, 2), Set(2, 3)) shouldBe Set(2)
      summon[Meet[LHSet[Int]]].meet(LHSet(1, 2), LHSet(2, 3)) shouldBe LHSet(2)
      summon[Meet[Option[Set[Int]]]]
        .meet(Some(Set(1, 2)), Some(Set(2, 3))) shouldBe Some(Set(2))
      summon[Meet[Option[Int]]].meet(Some(1), None) shouldBe None
    }

    "intersect qualifier references by reference name" in {
      val left = QuReferences(
        LHSet(
          QuReference(QU(LHSet('~', '!')), "shared"),
          QuReference(QU(LHSet('?')), "left")
        )
      )
      val right = QuReferences(
        LHSet(
          QuReference(QU(LHSet('!')), "shared"),
          QuReference(QU(LHSet('?')), "right")
        )
      )

      summon[Meet[QuReferences]].meet(left, right) shouldBe
        QuReferences(LHSet(QuReference(QU(LHSet('!')), "shared")))
    }

    "reject direct meets of unrelated atomic values" in {
      an[IllegalArgumentException] should be thrownBy {
        summon[Meet[QuReference]].meet(
          QuReference(refName = "left"),
          QuReference(refName = "right")
        )
      }

      an[IllegalArgumentException] should be thrownBy {
        summon[Meet[SingleValueUnit]].meet(
          SingleValueUnit(IntValue(1), "mg"),
          SingleValueUnit(IntValue(2), "mg")
        )
      }
    }
  }
