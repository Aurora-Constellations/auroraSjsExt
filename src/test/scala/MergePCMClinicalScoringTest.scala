package com.axiom.MergePCM

import com.axiom.testutils.FileUtils.*
import org.aurora.sjsast.GenAst
import org.aurora.sjsast.scoring.ScoreModuleResolver
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import typings.auroraLangium.cliMod.parse

import scala.concurrent.ExecutionContext
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.JSConverters.*

class MergePCMClinicalScoringTest extends AsyncWordSpec with Matchers {
  override implicit def executionContext: ExecutionContext = queue

  private val fixtureRoot = cwd / "src" / "test" / "resources" / "clinical_scoring"

  private def fixture(path: String): String =
    fixtureRoot / path

  "MergePCM.generateOrdersDSLResult" should {
    "resolve bundled score modules when no local override exists" in {
      for {
        parsed <- parse(fixture("bundled_fallback/gcs_severe_patient.aurora")).toFuture
        result <- MergePCM.generateOrdersDSLResult(parsed.asInstanceOf[GenAst.PCM])
      } yield {
        result.content should include("Scores:")
        result.content should include("gcs_total [7 _]")
        result.content should include("gcs_severity [severe _]")
        result.content should include("document_score_basis")
        result.warnings shouldBe Nil
      }
    }

    "prefer a local score module over the bundled fallback" in {
      for {
        parsed <- parse(fixture("local_override/gcs_severe_patient.aurora")).toFuture
        result <- MergePCM.generateOrdersDSLResult(parsed.asInstanceOf[GenAst.PCM])
      } yield {
        result.content should include("local_override_neuro_escalation")
        result.content should not include "document_score_basis"
        result.warnings shouldBe Nil
      }
    }

    "warn but still emit scores when bundled score modules are disabled" in {
      for {
        parsed <- parse(fixture("bundled_fallback/gcs_severe_patient.aurora")).toFuture
        result <- MergePCM.generateOrdersDSLResult(
          parsed.asInstanceOf[GenAst.PCM],
          ScoreModuleResolver.Options(bundledRoots = Nil)
        )
      } yield {
        result.content should include("Scores:")
        result.content should include("gcs_total [7 _]")
        result.content should not include "document_score_basis"
        result.warnings.exists(_.contains("gcs_severe")) shouldBe true
      }
    }

    "merge AF score outputs and bundled high-risk guidance into the final DSL" in {
      for {
        parsed <- parse(fixture("af_scores/af_high_risk_patient.aurora")).toFuture
        result <- MergePCM.generateOrdersDSLResult(parsed.asInstanceOf[GenAst.PCM])
      } yield {
        result.content should include("Scores:")
        result.content should include("cha2ds2_vasc_total [5 _]")
        result.content should include("cha2ds2_vasc_risk_band [high _]")
        result.content should include("review_documented_stroke_risk_factors")
        result.content should not include "chads2_"
        result.warnings shouldBe Nil
      }
    }

    "preserve the string-returning compatibility wrapper" in {
      for {
        parsed <- parse(fixture("bundled_fallback/gcs_severe_patient.aurora")).toFuture
        content <- MergePCM.generateOrdersDSL(parsed.asInstanceOf[GenAst.PCM])
      } yield {
        content should include("gcs_total [7 _]")
        content should include("document_score_basis")
      }
    }
  }
}
