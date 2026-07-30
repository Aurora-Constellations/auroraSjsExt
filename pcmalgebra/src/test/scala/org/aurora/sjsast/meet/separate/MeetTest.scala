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

   // ---- sets of qu refs and QUs ----

    "merge and intersect sets of QU into a single combined QU" in {
      val left = LHSet(QU(LHSet('a', 'b')), QU(LHSet('c')))
      val right = LHSet(QU(LHSet('b', 'c')))

      summon[Meet[LHSet[QU]]].meet(left, right) shouldBe
        LHSet(QU(LHSet('b', 'c')))
    }

    "return an empty set when merged QU sets share no characters" in {
      val left = LHSet(QU(LHSet('a')))
      val right = LHSet(QU(LHSet('b')))

      summon[Meet[LHSet[QU]]].meet(left, right) shouldBe LHSet.empty[QU]
    }

    "merge and intersect sets of QuReferences by reference name" in {
      val left = LHSet(
        QuReferences(LHSet(QuReference(QU(LHSet('~', '!')), "shared"))),
        QuReferences(LHSet(QuReference(QU(LHSet('?')), "left")))
      )
      val right = LHSet(
        QuReferences(
          LHSet(
            QuReference(QU(LHSet('!')), "shared"),
            QuReference(QU(LHSet('?')), "right")
          )
        )
      )

      summon[Meet[LHSet[QuReferences]]].meet(left, right) shouldBe
        LHSet(QuReferences(LHSet(QuReference(QU(LHSet('!')), "shared"))))
    }

    "return an empty set when merged QuReferences share no reference name" in {
      val left = LHSet(QuReferences(LHSet(QuReference(QU(), "a"))))
      val right = LHSet(QuReferences(LHSet(QuReference(QU(), "b"))))

      summon[Meet[LHSet[QuReferences]]].meet(left, right) shouldBe
        LHSet.empty[QuReferences]
    }

    // ---- named coordinates and groups ----

    "meet ClinicalCoordinate values with the same name" in {
      val left = ClinicalCoordinate(
        name = "temp",
        narratives = LHSet(NL_STATEMENT("high")),
        qurefs = LHSet(QuReferences(LHSet(QuReference(QU(LHSet('!')), "shared")))),
        qu = QU(LHSet('a', 'b'))
      )
      val right = ClinicalCoordinate(
        name = "temp",
        narratives = LHSet(NL_STATEMENT("high"), NL_STATEMENT("low")),
        qurefs = LHSet(QuReferences(LHSet(QuReference(QU(LHSet('!', '?')), "shared")))),
        qu = QU(LHSet('b', 'c'))
      )

      summon[Meet[ClinicalCoordinate]].meet(left, right) shouldBe
        ClinicalCoordinate(
          name = "temp",
          narratives = LHSet(NL_STATEMENT("high")),
          qurefs = LHSet(QuReferences(LHSet(QuReference(QU(LHSet('!')), "shared")))),
          qu = QU(LHSet('b'))
        )
    }

    "reject meets of ClinicalCoordinate values with different names" in {
      an[IllegalArgumentException] should be thrownBy {
        summon[Meet[ClinicalCoordinate]].meet(
          ClinicalCoordinate(name = "temp"),
          ClinicalCoordinate(name = "pulse")
        )
      }
    }

    "meet ClinicalValue values with the same name" in {
      val left = ClinicalValue(
        name = "wbc",
        values = List(SingleValueUnit(IntValue(1), "k"), SingleValueUnit(IntValue(2), "k")),
        narrative = LHSet(NL_STATEMENT("elevated"))
      )
      val right = ClinicalValue(
        name = "wbc",
        values = List(SingleValueUnit(IntValue(2), "k"), SingleValueUnit(IntValue(3), "k")),
        narrative = LHSet(NL_STATEMENT("elevated"))
      )

      summon[Meet[ClinicalValue]].meet(left, right) shouldBe
        ClinicalValue(
          name = "wbc",
          values = List(SingleValueUnit(IntValue(2), "k")),
          narrative = LHSet(NL_STATEMENT("elevated"))
        )
    }

    "reject meets of ClinicalValue values with different names" in {
      an[IllegalArgumentException] should be thrownBy {
        summon[Meet[ClinicalValue]].meet(
          ClinicalValue(name = "wbc"),
          ClinicalValue(name = "hgb")
        )
      }
    }

    "meet IssueCoordinate values with the same name" in {
      val left = IssueCoordinate(
        name = "chf",
        fromMods = List("cardiology", "icu"),
        narratives = LHSet(NL_STATEMENT("chronic")),
        qu = QU(LHSet('!'))
      )
      val right = IssueCoordinate(
        name = "chf",
        fromMods = List("icu", "ed"),
        narratives = LHSet(NL_STATEMENT("chronic")),
        qu = QU(LHSet('!', '?'))
      )

      summon[Meet[IssueCoordinate]].meet(left, right) shouldBe
        IssueCoordinate(
          name = "chf",
          fromMods = List("icu"),
          narratives = LHSet(NL_STATEMENT("chronic")),
          qu = QU(LHSet('!'))
        )
    }

    "reject meets of IssueCoordinate values with different names" in {
      an[IllegalArgumentException] should be thrownBy {
        summon[Meet[IssueCoordinate]].meet(
          IssueCoordinate(name = "chf"),
          IssueCoordinate(name = "copd")
        )
      }
    }

    "meet OrderCoordinate values with the same name" in {
      val left = OrderCoordinate(
        name = "cbc",
        narratives = LHSet(NL_STATEMENT("stat")),
        qurefs = LHSet(QuReferences(LHSet(QuReference(QU(LHSet('!')), "shared"))))
      )
      val right = OrderCoordinate(
        name = "cbc",
        narratives = LHSet(NL_STATEMENT("stat"), NL_STATEMENT("routine")),
        qurefs = LHSet(QuReferences(LHSet(QuReference(QU(LHSet('!', '?')), "shared"))))
      )

      summon[Meet[OrderCoordinate]].meet(left, right) shouldBe
        OrderCoordinate(
          name = "cbc",
          narratives = LHSet(NL_STATEMENT("stat")),
          qurefs = LHSet(QuReferences(LHSet(QuReference(QU(LHSet('!')), "shared"))))
        )
    }

    "reject meets of OrderCoordinate values with different names" in {
      an[IllegalArgumentException] should be thrownBy {
        summon[Meet[OrderCoordinate]].meet(
          OrderCoordinate(name = "cbc"),
          OrderCoordinate(name = "bmp")
        )
      }
    }

    "dispatch RefCoordinate meets to the matching subtype" in {
      val left: RefCoordinate = ClinicalCoordinate(name = "temp", qu = QU(LHSet('a', 'b')))
      val right: RefCoordinate = ClinicalCoordinate(name = "temp", qu = QU(LHSet('b', 'c')))

      summon[Meet[RefCoordinate]].meet(left, right) shouldBe
        ClinicalCoordinate(name = "temp", qu = QU(LHSet('b')))
    }

    "reject RefCoordinate meets across mismatched subtypes" in {
      val left: RefCoordinate = ClinicalCoordinate(name = "shared")
      val right: RefCoordinate = OrderCoordinate(name = "shared")

      an[IllegalArgumentException] should be thrownBy {
        summon[Meet[RefCoordinate]].meet(left, right)
      }
    }

    "intersect a set of RefCoordinate values by name" in {
      val left: LHSet[RefCoordinate] = LHSet(
        ClinicalCoordinate(name = "temp", qu = QU(LHSet('a', 'b'))),
        IssueCoordinate(name = "chf")
      )
      val right: LHSet[RefCoordinate] = LHSet(
        ClinicalCoordinate(name = "temp", qu = QU(LHSet('b', 'c'))),
        OrderCoordinate(name = "cbc")
      )

      summon[Meet[LHSet[RefCoordinate]]].meet(left, right) shouldBe
        LHSet(ClinicalCoordinate(name = "temp", qu = QU(LHSet('b'))))
    }

    "meet NGC groups by name, coordinates, and references" in {
      val left = NGC(
        name = "Vitals",
        narratives = LHSet(NL_STATEMENT("normal")),
        coordinates = LHSet(
          ClinicalCoordinate(name = "temp", qu = QU(LHSet('a', 'b'))),
          ClinicalValue(name = "hr", values = List(SingleValueUnit(IntValue(80), "bpm")))
        ),
        refs = LHSet(QuReferences(LHSet(QuReference(QU(LHSet('!')), "shared"))))
      )
      val right = NGC(
        name = "Vitals",
        narratives = LHSet(NL_STATEMENT("normal")),
        coordinates = LHSet(
          ClinicalCoordinate(name = "temp", qu = QU(LHSet('b', 'c'))),
          ClinicalValue(name = "hr", values = List(SingleValueUnit(IntValue(80), "bpm")))
        ),
        refs = LHSet(QuReferences(LHSet(QuReference(QU(LHSet('!', '?')), "shared"))))
      )

      summon[Meet[NGC]].meet(left, right) shouldBe
        NGC(
          name = "Vitals",
          narratives = LHSet(NL_STATEMENT("normal")),
          coordinates = LHSet(
            ClinicalCoordinate(name = "temp", qu = QU(LHSet('b'))),
            ClinicalValue(name = "hr", values = List(SingleValueUnit(IntValue(80), "bpm")))
          ),
          refs = LHSet(QuReferences(LHSet(QuReference(QU(LHSet('!')), "shared"))))
        )
    }

    "reject meets of NGC groups with different names" in {
      an[IllegalArgumentException] should be thrownBy {
        summon[Meet[NGC]].meet(NGC(name = "Vitals"), NGC(name = "Labs"))
      }
    }

    "meet NGO groups by name, orders, references, and QU" in {
      val left = NGO(
        name = "Labs",
        narratives = LHSet(NL_STATEMENT("stat")),
        ordercoord = LHSet(OrderCoordinate(name = "cbc"), OrderCoordinate(name = "bmp")),
        qu = LHSet(QU(LHSet('a')), QU(LHSet('b')))
      )
      val right = NGO(
        name = "Labs",
        narratives = LHSet(NL_STATEMENT("stat")),
        ordercoord = LHSet(OrderCoordinate(name = "cbc"), OrderCoordinate(name = "lft")),
        qu = LHSet(QU(LHSet('b')), QU(LHSet('c')))
      )

      summon[Meet[NGO]].meet(left, right) shouldBe
        NGO(
          name = "Labs",
          narratives = LHSet(NL_STATEMENT("stat")),
          ordercoord = LHSet(OrderCoordinate(name = "cbc")),
          qu = LHSet(QU(LHSet('b')))
        )
    }

    "reject meets of NGO groups with different names" in {
      an[IllegalArgumentException] should be thrownBy {
        summon[Meet[NGO]].meet(NGO(name = "Labs"), NGO(name = "Meds"))
      }
    }

    "intersect sets of NGC and NGO groups by name" in {
      val leftNgc = LHSet(NGC(name = "Vitals"), NGC(name = "History"))
      val rightNgc = LHSet(NGC(name = "Vitals"))
      summon[Meet[LHSet[NGC]]].meet(leftNgc, rightNgc) shouldBe LHSet(NGC(name = "Vitals"))

      val leftNgo = LHSet(NGO(name = "Labs"), NGO(name = "Imaging"))
      val rightNgo = LHSet(NGO(name = "Labs"))
      summon[Meet[LHSet[NGO]]].meet(leftNgo, rightNgo) shouldBe LHSet(NGO(name = "Labs"))
    }
  }
