package org.aurora.sjsast

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ClinicalScoringTest extends AnyWordSpec with Matchers {

  "ClinicalScoring" should {

    "derive a severe adult GCS score and score-driven module import" in {
      val pcm = PCM(
        cio = LHMap(
          "Clinical" -> Clinical(
            ngc = LHSet(
              NGC(
                name = "Neurologic:",
                coordinates = LHSet(
                  intValue("gcs_eye", 2),
                  intValue("gcs_verbal", 2),
                  intValue("gcs_motor", 3)
                )
              )
            )
          )
        )
      )

      val update = ClinicalScoring(pcm)

      scoreValue(update.pcm, "gcs_total") shouldBe Some(IntValue(7))
      scoreValue(update.pcm, "gcs_total_source") shouldBe Some(StringValue("derived"))
      scoreValue(update.pcm, "gcs_severity") shouldBe Some(StringValue("severe"))

      derivedIssue(update.pcm, "score_gcs_severe").map(_.fromMods) shouldBe Some(List("gcs_severe"))
      update.derivedModuleImports shouldBe Map("gcs_severe" -> "score_gcs_severe")
    }

    "derive CHA2DS2-VASc from patient demographics and active issues" in {
      val pcm = PCM(
        cio = LHMap(
          "Clinical" -> Clinical(
            ngc = LHSet(
              NGC(
                name = "Demographics:",
                coordinates = LHSet(
                  intValue("age", 76, "yr"),
                  textValue("sex", "female"),
                  textValue("cha2ds2_vasc_diabetes", "absent"),
                  textValue("prior_stroke_tia_te", "absent"),
                  textValue("vascular_disease", "absent")
                )
              )
            )
          ),
          "Issues" -> Issues(
            ic = LHSet(
              IssueCoordinate("atrial_fibrillation"),
              IssueCoordinate("heart_failure"),
              IssueCoordinate("hypertension")
            )
          )
        )
      )

      val update = ClinicalScoring(pcm)

      scoreValue(update.pcm, "cha2ds2_vasc_total") shouldBe Some(IntValue(5))
      scoreValue(update.pcm, "cha2ds2_vasc_risk_band") shouldBe Some(StringValue("high"))

      derivedIssue(update.pcm, "score_af_stroke_risk_high").map(_.fromMods) shouldBe Some(List("af_stroke_risk_high"))
      update.derivedModuleImports shouldBe Map("af_stroke_risk_high" -> "score_af_stroke_risk_high")
    }

    "record not-testable GCS without forcing a computed total" in {
      val pcm = PCM(
        cio = LHMap(
          "Clinical" -> Clinical(
            ngc = LHSet(
              NGC(
                name = "Neurologic:",
                coordinates = LHSet(
                  intValue("gcs_eye", 2),
                  textValue("gcs_verbal", "NT"),
                  intValue("gcs_motor", 5)
                )
              )
            )
          )
        )
      )

      val update = ClinicalScoring(pcm)

      scoreValue(update.pcm, "gcs_total") shouldBe None
      scoreValue(update.pcm, "gcs_status") shouldBe Some(StringValue("not_testable"))
      derivedIssue(update.pcm, "score_gcs_severe") shouldBe None
      derivedIssue(update.pcm, "score_gcs_moderate") shouldBe None
    }

    "ignore manual GCS totals when a component is not testable" in {
      val pcm = PCM(
        cio = LHMap(
          "Clinical" -> Clinical(
            ngc = LHSet(
              NGC(
                name = "Neurologic:",
                coordinates = LHSet(
                  intValue("gcs_eye", 2),
                  textValue("gcs_verbal", "NT"),
                  intValue("gcs_motor", 5),
                  intValue("gcs_total", 7)
                )
              )
            )
          )
        )
      )

      val update = ClinicalScoring(pcm)

      scoreValue(update.pcm, "gcs_total") shouldBe None
      scoreValue(update.pcm, "gcs_total_source") shouldBe None
      scoreValue(update.pcm, "gcs_severity") shouldBe None
      scoreValue(update.pcm, "gcs_status") shouldBe Some(StringValue("not_testable"))
    }

    "withhold CHA2DS2-VASc when required risk factors are unknown" in {
      val pcm = PCM(
        cio = LHMap(
          "Clinical" -> Clinical(
            ngc = LHSet(
              NGC(
                name = "Demographics:",
                coordinates = LHSet(
                  intValue("age", 76, "yr"),
                  textValue("sex", "female")
                )
              )
            )
          ),
          "Issues" -> Issues(
            ic = LHSet(
              IssueCoordinate("atrial_fibrillation"),
              IssueCoordinate("heart_failure")
            )
          )
        )
      )

      val update = ClinicalScoring(pcm)

      scoreValue(update.pcm, "cha2ds2_vasc_total") shouldBe None
      scoreValue(update.pcm, "cha2ds2_vasc_risk_band") shouldBe None
      scoreValue(update.pcm, "cha2ds2_vasc_status") shouldBe Some(StringValue("insufficient_data"))
      derivedIssue(update.pcm, "score_af_stroke_risk_high") shouldBe None
      derivedIssue(update.pcm, "score_af_stroke_risk_intermediate") shouldBe None
    }

    "be idempotent across repeated scoring passes" in {
      val pcm = PCM(
        cio = LHMap(
          "Clinical" -> Clinical(
            ngc = LHSet(
              NGC(
                name = "Demographics:",
                coordinates = LHSet(
                  intValue("age", 72, "yr"),
                  textValue("sex", "male"),
                  textValue("cha2ds2_vasc_heart_failure", "absent"),
                  textValue("cha2ds2_vasc_hypertension", "absent"),
                  textValue("cha2ds2_vasc_diabetes", "present"),
                  textValue("prior_stroke_tia_te", "absent"),
                  textValue("vascular_disease", "absent")
                )
              ),
              NGC(
                name = "Neurologic:",
                coordinates = LHSet(
                  intValue("gcs_eye", 4),
                  intValue("gcs_verbal", 4),
                  intValue("gcs_motor", 5)
                )
              )
            )
          ),
          "Issues" -> Issues(
            ic = LHSet(
              IssueCoordinate("atrial_fibrillation"),
              IssueCoordinate("diabetes")
            )
          )
        )
      )

      val once = ClinicalScoring(pcm)
      val twice = ClinicalScoring(once.pcm)

      scoresGroupCount(once.pcm) shouldBe 1
      scoresGroupCount(twice.pcm) shouldBe 1
      once.pcm shouldBe twice.pcm
      once.derivedModuleImports shouldBe twice.derivedModuleImports
    }
  }

  private def intValue(name: String, value: Int, unit: String = "_"): ClinicalValue =
    ClinicalValue(name = name, values = List(SingleValueUnit(IntValue(value), unit)))

  private def textValue(name: String, value: String): ClinicalValue =
    ClinicalValue(name = name, values = List(SingleValueUnit(StringValue(value), "_")))

  private def scoreValue(pcm: PCM, name: String): Option[Value] =
    pcm.cio
      .get("Clinical")
      .collect { case clinical: Clinical => clinical }
      .flatMap(_.ngc.find(_.name == "Scores:"))
      .flatMap(_.coordinates.collectFirst {
        case value: ClinicalValue if value.name == name && value.values.nonEmpty => value.values.head.value
      })

  private def derivedIssue(pcm: PCM, name: String): Option[IssueCoordinate] =
    pcm.cio
      .get("Issues")
      .collect { case issues: Issues => issues }
      .flatMap(_.ic.find(_.name == name))

  private def scoresGroupCount(pcm: PCM): Int =
    pcm.cio
      .get("Clinical")
      .collect { case clinical: Clinical => clinical.ngc.count(_.name == "Scores:") }
      .getOrElse(0)
}
