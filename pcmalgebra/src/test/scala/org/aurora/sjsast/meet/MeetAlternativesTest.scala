package org.aurora.sjsast.meet

import org.aurora.sjsast.meet.combined.JoinMeet
import org.aurora.sjsast.meet.combined.JoinMeet.given
import org.aurora.sjsast.meet.tagged.{Combine, Operation}
import org.aurora.sjsast.meet.tagged.Combine.given
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class MeetAlternativesTest extends AnyWordSpec with Matchers:

  "Combined JoinMeet" should {
    "provide join and meet for its supported types" in {
      summon[JoinMeet[Int]].join(2, 3) shouldBe 5
      summon[JoinMeet[Int]].meet(2, 3) shouldBe 2
      summon[JoinMeet[Boolean]].meet(true, false) shouldBe false
      summon[JoinMeet[Set[Int]]].meet(Set(1, 2), Set(2, 3)) shouldBe Set(2)
    }
  }

  "Tagged Combine" should {
    "select join or meet through the operation tag" in {
      summon[Combine[Int]].combine(Operation.Join, 2, 3) shouldBe 5
      summon[Combine[Int]].combine(Operation.Meet, 2, 3) shouldBe 2
      summon[Combine[Boolean]].combine(Operation.Meet, true, false) shouldBe false
      summon[Combine[Set[Int]]].combine(Operation.Meet, Set(1, 2), Set(2, 3)) shouldBe Set(2)
    }
  }
