package org.aurora.sjsast.scoring

import org.aurora.sjsast.*
import org.aurora.sjsast.scoring.af.Cha2ds2VascRiskBand
import org.aurora.sjsast.scoring.gcs.{GcsSeverity, GcsStatus, GcsTotalSource}

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ScoringPatchesTest extends AnyWordSpec with Matchers:

  "ScoringPatches" should {
    "write derived GCS scores into the Scores group" in {
      val pcm = pcmWithFacts(
        Map(
          "gcs_eye" -> textValue("to voice"),
          "gcs_verbal" -> textValue("confused"),
          "gcs_motor" -> textValue("withdraws")
        )
      )

      val scores = extractScoreValues(ScoringPatches.applyDerivedScores(pcm))

      scores.get("gcs_total").map(_.head.value).shouldBe(Some(IntValue(11)))
      scores.get("gcs_severity").map(_.head.value).shouldBe(Some(StringValue("moderate")))
      scores.get("gcs_total_source").map(_.head.value).shouldBe(Some(StringValue("derived")))
      scores.contains("gcs_status").shouldBe(false)
    }

    "write derived CHA2DS2-VASc scores into the Scores group" in {
      val pcm = pcmWithFacts(
        Map(
          "age" -> textValue("76"),
          "sex" -> textValue("female"),
          "cha2ds2_vasc_diabetes" -> textValue("absent"),
          "prior_stroke_tia_te" -> textValue("absent"),
          "vascular_disease" -> textValue("absent")
        ),
        issueNames = Set("atrial_fibrillation", "heart_failure", "hypertension")
      )

      val scores = extractScoreValues(ScoringPatches.applyDerivedScores(pcm))

      scores.get("cha2ds2_vasc_total").map(_.head.value).shouldBe(Some(IntValue(5)))
      scores.get("cha2ds2_vasc_risk_band").map(_.head.value).shouldBe(Some(StringValue("high")))
      scores.contains("cha2ds2_vasc_status").shouldBe(false)
    }
  }

  private def textValue(value: String): SingleValueUnit =
    SingleValueUnit(StringValue(value), ScoringConstants.PlaceholderUnit)

  private def pcmWithFacts(
      values: Map[String, SingleValueUnit],
      issueNames: Set[String] = Set.empty
  ): PCM =
    val clinicalValues =
      values.map { case (name, value) =>
        ClinicalValue(name = name, values = List(value))
      }

    PCM(
      cio = LHMap(
        "Clinical" -> Clinical(
          ngc = LHSet(
            NGC(
              name = "Facts:",
              coordinates = LHSet.from(clinicalValues)
            )
          )
        ),
        "Issues" -> Issues(
          ic = LHSet.from(issueNames.map(name => IssueCoordinate(name = name)))
        )
      )
    )

  private def extractScoreValues(pcm: PCM): Map[String, List[SingleValueUnit]] =
    pcm.cio
      .get("Clinical")
      .collect { case clinical: Clinical => clinical }
      .flatMap(_.ngc.find(_.name == ScoringConstants.ScoreGroupName))
      .map { scoresGroup =>
        scoresGroup.coordinates.collect {
          case value: ClinicalValue => value.name -> value.values
        }.toMap
      }
      .getOrElse(Map.empty)