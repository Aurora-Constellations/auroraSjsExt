package org.aurora.sjsast.scoring

import org.aurora.sjsast.*
import org.aurora.sjsast.scoring.af.Cha2ds2VascRiskBand
import org.aurora.sjsast.scoring.gcs.{GcsSeverity, GcsStatus, GcsTotalSource}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ScoringPatchesTest extends AnyWordSpec with Matchers:

  "ScoringPatches" should {

    "derive GCS total, severity and source when all components are resolvable" in {
      val pcm = pcmWithFacts(
        Map(
          "gcs_eye" -> textValue("spontaneous"),
          "gcs_verbal" -> textValue("oriented"),
          "gcs_motor" -> textValue("obeys commands")
        )
      )

      val patched = ScoringPatches.applyDerivedScores(pcm)
      val scores = extractScoreValues(patched)

      scores.get("gcs_total").map(_.head.value).shouldBe(Some(IntValue(15)))
      scores.get("gcs_severity").map(_.head.value)
        .shouldBe(Some(StringValue(GcsSeverity.Mild.outputValue)))
      scores.get("gcs_total_source").map(_.head.value)
        .shouldBe(Some(StringValue(GcsTotalSource.Derived.outputValue)))
      scores.contains("gcs_status").shouldBe(false)
    }

    "mark GCS status as not_testable when any component is NT" in {
      val pcm = pcmWithFacts(
        Map(
          "gcs_eye" -> textValue("NT"),
          "gcs_verbal" -> textValue("oriented"),
          "gcs_motor" -> textValue("obeys commands")
        )
      )

      val patched = ScoringPatches.applyDerivedScores(pcm)
      val scores = extractScoreValues(patched)

      scores.get("gcs_status").map(_.head.value)
        .shouldBe(Some(StringValue(GcsStatus.NotTestable.outputValue)))
      scores.contains("gcs_total").shouldBe(false)
    }

    "produce no GCS derived values when no GCS data is present" in {
      val pcm = pcmWithFacts(Map.empty)

      val patched = ScoringPatches.applyDerivedScores(pcm)
      val scores = extractScoreValues(patched)

      scores.keys.exists(_.startsWith("gcs_")).shouldBe(false)
    }

    "derive AF total and high risk band when atrial fibrillation is confirmed" in {
      val pcm = pcmWithFactsAndIssues(
        values = Map(
          "age" -> textValue("76"),
          "sex" -> textValue("female"),
          "cha2ds2_vasc_diabetes" -> textValue("absent"),
          "prior_stroke_tia_te" -> textValue("absent"),
          "vascular_disease" -> textValue("absent")
        ),
        issueNames = Set("atrial_fibrillation", "heart_failure", "hypertension")
      )

      val patched = ScoringPatches.applyDerivedScores(pcm)
      val scores = extractScoreValues(patched)

      scores.get("af_cha2ds2_vasc_total").map(_.head.value).shouldBe(Some(IntValue(5)))
      scores.get("af_cha2ds2_vasc_risk_band").map(_.head.value)
        .shouldBe(Some(StringValue(Cha2ds2VascRiskBand.High.outputValue)))
      scores.contains("af_cha2ds2_vasc_status").shouldBe(false)
    }

    "report AF insufficient_data when diagnosis is present but required factors are missing" in {
      val pcm = pcmWithFactsAndIssues(
        values = Map(
          "age" -> textValue("70"),
          "sex" -> textValue("female")
        ),
        issueNames = Set("atrial_fibrillation")
      )

      val patched = ScoringPatches.applyDerivedScores(pcm)
      val scores = extractScoreValues(patched)

      scores.get("af_cha2ds2_vasc_status").map(_.head.value)
        .shouldBe(Some(StringValue("insufficient_data")))
      scores.contains("af_cha2ds2_vasc_total").shouldBe(false)
    }

    "produce no AF derived values when atrial fibrillation is absent" in {
      val pcm = pcmWithFactsAndIssues(
        values = Map(
          "age" -> textValue("76"),
          "sex" -> textValue("female")
        ),
        issueNames = Set.empty
      )

      val patched = ScoringPatches.applyDerivedScores(pcm)
      val scores = extractScoreValues(patched)

      scores.keys.exists(_.startsWith("af_")).shouldBe(false)
    }

    "combine GCS and AF derived values under the same Scores group without losing original facts" in {
      val pcm = pcmWithFactsAndIssues(
        values = Map(
          "age" -> textValue("76"),
          "sex" -> textValue("female"),
          "cha2ds2_vasc_diabetes" -> textValue("absent"),
          "prior_stroke_tia_te" -> textValue("absent"),
          "vascular_disease" -> textValue("absent"),
          "gcs_eye" -> textValue("spontaneous"),
          "gcs_verbal" -> textValue("oriented"),
          "gcs_motor" -> textValue("obeys commands")
        ),
        issueNames = Set("atrial_fibrillation", "heart_failure", "hypertension")
      )

      val patched = ScoringPatches.applyDerivedScores(pcm)
      val scores = extractScoreValues(patched)

      scores.contains("gcs_total").shouldBe(true)
      scores.contains("af_cha2ds2_vasc_total").shouldBe(true)

      val originalFactsPreserved =
        patched.cio.get("Clinical").collect { case c: Clinical => c }
          .exists(_.ngc.exists(_.name != ScoringConstants.ScoreGroupName))
      originalFactsPreserved.shouldBe(true)
    }
  }

  private def textValue(value: String): SingleValueUnit =
    SingleValueUnit(StringValue(value), ScoringConstants.PlaceholderUnit)

  private def pcmWithFacts(values: Map[String, SingleValueUnit]): PCM =
    pcmWithFactsAndIssues(values, Set.empty)

  private def pcmWithFactsAndIssues(
      values: Map[String, SingleValueUnit],
      issueNames: Set[String]
  ): PCM =
    val clinicalValues: List[ClinicalValue] =
      values.map { case (name, value) =>
        ClinicalValue(name = name, values = List(value))
      }.toList

    val factsGroup = NGC(
      name = "Facts:",
      coordinates = LHSet[RefCoordinate](clinicalValues*)
    )
    val clinical = Clinical(ngc = LHSet(factsGroup))

    val issueCoordinates = issueNames.map(n => IssueCoordinate(name = n)).toList
    val issues = Issues(ic = LHSet(issueCoordinates*))

    PCM(cio = LHMap[String, CIO]("Clinical" -> clinical, "Issues" -> issues))

  private def extractScoreValues(pcm: PCM): Map[String, List[SingleValueUnit]] =
    pcm.cio.get("Clinical").collect { case c: Clinical => c } match
      case None => Map.empty
      case Some(clinical) =>
        clinical.ngc
          .filter(_.name == ScoringConstants.ScoreGroupName)
          .flatMap(_.coordinates)
          .collect { case v: ClinicalValue => v.name -> v.values }
          .toMap