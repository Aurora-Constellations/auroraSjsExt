package org.aurora.sjsast.scoring

import org.aurora.sjsast.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ScoringPatchesTest extends AnyWordSpec with Matchers:

  "ScoringPatches" should {
    "write the complete derived GCS result through the compatibility API" in {
      val pcm = pcmWithFacts(
        Map(
          "gcs_eye" -> textValue("to voice"),
          "gcs_verbal" -> textValue("confused"),
          "gcs_motor" -> textValue("withdraws")
        )
      )

      val scored = ScoringPatches.applyDerivedScores(pcm)

      scoreValue(scored, "gcs_total") shouldBe Some(IntValue(11))
      scoreValue(scored, "gcs_severity") shouldBe Some(StringValue("moderate"))
      scoreValue(scored, "gcs_total_source") shouldBe Some(StringValue("derived"))
      scoreValue(scored, "gcs_status") shouldBe None
      derivedIssue(scored, "score_gcs_moderate") shouldBe defined
    }

    "write derived CHA2DS2-VASc values and guidance issue" in {
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

      val scored = ScoringPatches.applyDerivedScores(pcm)

      scoreValue(scored, "cha2ds2_vasc_total") shouldBe Some(IntValue(5))
      scoreValue(scored, "cha2ds2_vasc_risk_band") shouldBe Some(StringValue("high"))
      scoreValue(scored, "cha2ds2_vasc_status") shouldBe None
      derivedIssue(scored, "score_af_stroke_risk_high") shouldBe defined
    }
  }

  private def textValue(value: String): SingleValueUnit =
    SingleValueUnit(StringValue(value), ScoringConstants.PlaceholderUnit)

  private def pcmWithFacts(
      values: Map[String, SingleValueUnit],
      issueNames: Set[String] = Set.empty
  ): PCM =
    PCM(
      cio = LHMap(
        "Clinical" -> Clinical(
          ngc = LHSet(
            NGC(
              name = "Facts:",
              coordinates = LHSet.from(
                values.map { case (name, value) =>
                  ClinicalItem(name = name, values = List(value))
                }
              )
            )
          )
        ),
        "Issues" -> Issues(
          ic = LHSet.from(issueNames.map(name => IssueCoordinate(name = name)))
        )
      )
    )

  private def scoreValue(pcm: PCM, name: String): Option[Value] =
    ScoreWriteback.scoreValue(pcm, name)

  private def derivedIssue(pcm: PCM, name: String): Option[IssueCoordinate] =
    pcm.cio
      .get("Issues")
      .collect { case issues: Issues => issues }
      .flatMap(_.ic.find(_.name == name))
