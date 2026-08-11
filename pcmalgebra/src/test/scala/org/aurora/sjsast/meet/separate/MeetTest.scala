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

    "intersect typed clinical coordinate collections by name" in {
      val commonNote = NL_STATEMENT("shared note")
      val commonValue = SingleValueUnit(IntValue(20), "years")

      val leftCoordinates = LHSet(
        ClinicalCoordinate(
          name = "status",
          narratives = LHSet(commonNote, NL_STATEMENT("left note"))
        )
      )
      val rightCoordinates = LHSet(
        ClinicalCoordinate(
          name = "status",
          narratives = LHSet(commonNote, NL_STATEMENT("right note"))
        )
      )
      val leftValues = LHSet(
        ClinicalValue(
          name = "age",
          values = List(commonValue, SingleValueUnit(IntValue(10), "years"))
        )
      )
      val rightValues = LHSet(
        ClinicalValue(
          name = "age",
          values = List(commonValue, SingleValueUnit(IntValue(30), "years"))
        )
      )

      (leftCoordinates |&| rightCoordinates) shouldBe
        LHSet(ClinicalCoordinate(name = "status", narratives = LHSet(commonNote)))
      (leftValues |&| rightValues) shouldBe
        LHSet(ClinicalValue(name = "age", values = List(commonValue)))
    }

    "omit a common map key when section variants disagree" in {
      val left = PCM(LHMap("section" -> Clinical()))
      val right = PCM(LHMap("section" -> Orders()))

      summon[Meet[PCM]].meet(left, right).cio shouldBe empty
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
