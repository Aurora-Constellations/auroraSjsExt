package org.aurora.sjsast.scoring

import org.aurora.sjsast.*
import org.aurora.sjsast.Show.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ScoringPrecheckTest extends AnyWordSpec with Matchers:
  "ScoringPrecheck" should {
    "leave unsupported scores inactive and preserve the input PCM" in {
      val pcm = patient(Seq(intItem("age", 72)), Nil)
      val original = pcm.show

      val report = ScoringPrecheck.validate(pcm)

      report.gcs.status shouldBe ScoringPrecheckStatus.NotApplicable
      report.cha2ds2Vasc.status shouldBe ScoringPrecheckStatus.NotApplicable
      report.isValid shouldBe true
      pcm.show shouldBe original
    }

    "name missing GCS components but accept a valid manual total" in {
      val partial = patient(Seq(intItem("gcs_eye", 3)), Nil)
      val incomplete = ScoringPrecheck.validate(partial).gcs

      incomplete.status shouldBe ScoringPrecheckStatus.Incomplete
      incomplete.issues.map(issue => issue.field -> issue.problem) shouldBe List(
        "gcs_verbal" -> ScoringInputProblem.Missing,
        "gcs_motor" -> ScoringInputProblem.Missing
      )

      val withManualTotal = patient(Seq(textItem("gcs_eye", "bad"), intItem("gcs_total", 13)), Nil)
      ScoringPrecheck.validate(withManualTotal).gcs.status shouldBe ScoringPrecheckStatus.Ready
    }

    "accept not-testable GCS even when a manual total is present" in {
      val pcm = patient(Seq(textItem("gcs_verbal", "NT"), intItem("gcs_total", 7)), Nil)

      val check = ScoringPrecheck.validate(pcm).gcs

      check.status shouldBe ScoringPrecheckStatus.NotTestable
      check.issues shouldBe empty
      ScoreWriteback.scoreValue(ClinicalScoring(pcm).pcm, "gcs_status") shouldBe
        Some(StringValue("not_testable"))
      ScoringPrecheck.validate(pcm).isValid shouldBe true
    }

    "report an invalid manual GCS total when no complete component set exists" in {
      val pcm = patient(Seq(intItem("gcs_total", 21)), Nil)

      val check = ScoringPrecheck.validate(pcm).gcs

      check.status shouldBe ScoringPrecheckStatus.Incomplete
      check.issues should contain(ScoringInputIssue(
        "gcs_total",
        ScoringInputProblem.Invalid,
        "manual GCS total from 3 to 15"
      ))
    }

    "require AF risk inputs only after a confirmed diagnosis and identify invalid inputs" in {
      val absent = patient(Seq(textItem("atrial_fibrillation", "absent")), Nil)
      ScoringPrecheck.validate(absent).cha2ds2Vasc.status shouldBe ScoringPrecheckStatus.NotApplicable

      val confirmed = patient(
        Seq(intItem("age", -1), textItem("sex", "unknown"), textItem("diabetes", "unclear")),
        List("atrial_fibrillation", "heart_failure")
      )
      val check = ScoringPrecheck.validate(confirmed).cha2ds2Vasc

      check.status shouldBe ScoringPrecheckStatus.Incomplete
      check.issues.map(issue => issue.field -> issue.problem) shouldBe List(
        "age" -> ScoringInputProblem.Invalid,
        "sex" -> ScoringInputProblem.Invalid,
        "hypertension" -> ScoringInputProblem.Missing,
        "diabetes" -> ScoringInputProblem.Invalid,
        "prior_stroke_tia_te" -> ScoringInputProblem.Missing,
        "vascular_disease" -> ScoringInputProblem.Missing
      )
    }

    "accept issue-derived positives and explicit negatives as complete AF inputs" in {
      val pcm = patient(
        Seq(
          intItem("age", 76),
          textItem("sex", "female"),
          textItem("heart_failure", "absent"),
          textItem("hypertension", "absent"),
          textItem("diabetes", "absent"),
          textItem("prior_stroke_tia_te", "absent"),
          textItem("vascular_disease", "absent")
        ),
        List("atrial_fibrillation", "hypertension")
      )

      val check = ScoringPrecheck.validate(pcm).cha2ds2Vasc

      check.status shouldBe ScoringPrecheckStatus.Ready
      check.issues shouldBe empty
      ScoreWriteback.scoreValue(ClinicalScoring(pcm).pcm, "cha2ds2_vasc_total") shouldBe
        Some(IntValue(3))
    }

    "identify an invalid diagnosis without demanding unrelated risk factors" in {
      val pcm = patient(Seq(textItem("atrial_fibrillation", "maybe")), Nil)

      val check = ScoringPrecheck.validate(pcm).cha2ds2Vasc

      check.status shouldBe ScoringPrecheckStatus.Incomplete
      check.issues.map(_.field) shouldBe List("atrial_fibrillation")
      check.issues.head.problem shouldBe ScoringInputProblem.Invalid
    }

    "treat the age placeholder inserted before scoring as a missing value" in {
      val pcm = ParametricModeling.applyAgeConstraint(patient(Nil, List("atrial_fibrillation")))

      val check = ScoringPrecheck.validate(pcm).cha2ds2Vasc

      check.status shouldBe ScoringPrecheckStatus.Incomplete
      check.issues.find(_.field == "age").map(_.problem) shouldBe Some(ScoringInputProblem.Missing)
    }
  }

  private def patient(items: Seq[ClinicalItem], issues: List[String]): PCM =
    PCM(cio = LHMap(
      "Clinical" -> Clinical(ngc = LHSet(NGC(name = "Findings:", coordinates = LHSet.from(items)))),
      "Issues" -> Issues(ic = LHSet.from(issues.map(IssueCoordinate(_))))
    ))

  private def intItem(name: String, value: Int): ClinicalItem =
    ClinicalItem(name = name, values = List(SingleValueUnit(IntValue(value), "_")))

  private def textItem(name: String, value: String): ClinicalItem =
    ClinicalItem(name = name, values = List(SingleValueUnit(StringValue(value), "_")))
