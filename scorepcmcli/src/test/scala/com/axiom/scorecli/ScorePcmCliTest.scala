package com.axiom.scorecli

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import scala.concurrent.ExecutionContext
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

class ScorePcmCliTest extends AsyncWordSpec with Matchers:
  override implicit def executionContext: ExecutionContext = queue

  private val fixtureRoot = "scorepcmcli/src/test/resources/scorepcmcli"

  private def fixture(name: String): String =
    s"$fixtureRoot/$name"

  "ScorePcmCli" should {

    "score a severe GCS PCM" in {
      ScorePcmCli.scoreFile(fixture("gcs-severe.aurora")).map { report =>
        report.file shouldBe "gcs-severe.aurora"
        report.scores.GCS_Adult.status shouldBe "computable"
        report.scores.GCS_Adult.gcs_total shouldBe Some(7)
        report.scores.GCS_Adult.gcs_total_source shouldBe Some("derived")
        report.scores.GCS_Adult.gcs_severity shouldBe Some("severe")
        report.scores.CHA2DS2_VASc.status shouldBe "not_present"
      }
    }

    "score a high AF-risk PCM" in {
      ScorePcmCli.scoreFile(fixture("af-high.aurora")).map { report =>
        report.scores.CHA2DS2_VASc.status shouldBe "computable"
        report.scores.CHA2DS2_VASc.cha2ds2_vasc_total shouldBe Some(5)
        report.scores.CHA2DS2_VASc.cha2ds2_vasc_risk_band shouldBe Some("high")
      }
    }

    "report insufficient AF data" in {
      ScorePcmCli.scoreFile(fixture("af-insufficient.aurora")).map { report =>
        report.scores.CHA2DS2_VASc.status shouldBe "insufficient_data"
        report.scores.CHA2DS2_VASc.cha2ds2_vasc_total shouldBe None
        report.scores.CHA2DS2_VASc.cha2ds2_vasc_risk_band shouldBe None
      }
    }

    "fail on a missing file path" in {
      recoverToSucceededIf[IllegalArgumentException] {
        ScorePcmCli.scoreFile(fixture("missing-file.aurora"))
      }
    }
  }
