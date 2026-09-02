package org.aurora.sjsast.scoring.summary

import org.aurora.sjsast.*
import org.aurora.sjsast.scoring.{ClinicalScoring, ScoreWriteback}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ScoreSummaryTest extends AnyWordSpec with Matchers:

  "ScoreSummary" should {
    "report computed and absent scores from an already-scored PCM" in {
      val scored = ClinicalScoring(
        PCM(
          cio = LHMap(
            "Clinical" -> Clinical(
              ngc = LHSet(
                NGC(
                  name = "Neurologic:",
                  coordinates = LHSet(
                    intItem("gcs_eye", 4),
                    intItem("gcs_verbal", 5),
                    intItem("gcs_motor", 6)
                  )
                )
              )
            )
          )
        )
      ).pcm

      val summary = ScoreSummary.fromScoredPcm(scored)

      summary.GCS_Adult shouldBe GcsAdultSummary(
        status = "computable",
        gcs_total = Some(15),
        gcs_total_source = Some("derived"),
        gcs_severity = Some("mild")
      )
      summary.CHA2DS2_VASc shouldBe Cha2Ds2VascSummary(status = "not_present")
    }

    "surface non-computable scorer statuses" in {
      val scored = ScoreWriteback(
        PCM(),
        List(ScoreWriteback.textScore("gcs_status", "incomplete")),
        Nil
      )

      ScoreSummary.fromScoredPcm(scored).GCS_Adult shouldBe
        GcsAdultSummary(status = "incomplete")
    }
  }

  private def intItem(name: String, value: Int): ClinicalItem =
    ClinicalItem(name = name, values = List(SingleValueUnit(IntValue(value), "_")))
