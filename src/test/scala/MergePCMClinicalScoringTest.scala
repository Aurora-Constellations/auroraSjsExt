package com.axiom.MergePCM

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import scala.concurrent.ExecutionContext
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.JSConverters.*

import com.axiom.testutils.FileUtils.*
import org.aurora.sjsast.GenAst
import typings.auroraLangium.cliMod.parse

class MergePCMClinicalScoringTest extends AsyncWordSpec with Matchers {
  override implicit def executionContext: ExecutionContext = queue

  private val fixtureRoot = cwd / "src" / "test" / "resources" / "clinical_scoring"
  private val bundledRoot = cwd / "score-modules"

  private def fixture(path: String): String =
    fixtureRoot / path

  "MergePCM.generateOrdersDSL" should {

    "resolve bundled score modules when no local override exists" in {
      for {
        parsed <- parse(fixture("bundled_fallback/gcs_severe_patient.aurora")).toFuture
        result <- MergePCM.generateOrdersDSL(
          parsed.asInstanceOf[GenAst.PCM],
          bundledScoreModuleRoots = List(bundledRoot)
        )
      } yield {
        result.content should include("Assessments:")
        result.content should include("score GCS_Adult")
        result.content should include("gcs_total [7 _]")
        result.content should include("gcs_severity [severe _]")
        result.content should include("status [computable _]")
        result.content should include("document_score_basis")
        result.warnings shouldBe Nil
      }
    }

    "prefer a local score module over the bundled fallback" in {
      for {
        parsed <- parse(fixture("local_override/gcs_severe_patient.aurora")).toFuture
        result <- MergePCM.generateOrdersDSL(
          parsed.asInstanceOf[GenAst.PCM],
          bundledScoreModuleRoots = List(bundledRoot)
        )
      } yield {
        result.content should include("local_override_neuro_escalation")
        result.content should not include "document_score_basis"
        result.warnings shouldBe Nil
      }
    }

    "warn but still emit scores when a score module cannot be resolved" in {
      for {
        parsed <- parse(fixture("bundled_fallback/gcs_severe_patient.aurora")).toFuture
        result <- MergePCM.generateOrdersDSL(
          parsed.asInstanceOf[GenAst.PCM],
          bundledScoreModuleRoots = Nil
        )
      } yield {
        result.content should include("Assessments:")
        result.content should include("gcs_total [7 _]")
        result.content should not include "document_score_basis"
        result.warnings.exists(_.contains("gcs_severe")) shouldBe true
      }
    }

    "merge AF score outputs and bundled high-risk guidance into the final DSL" in {
      for {
        parsed <- parse(fixture("af_scores/af_high_risk_patient.aurora")).toFuture
        result <- MergePCM.generateOrdersDSL(
          parsed.asInstanceOf[GenAst.PCM],
          bundledScoreModuleRoots = List(bundledRoot)
        )
      } yield {
        result.content should include("Assessments:")
        result.content should include("score CHA2DS2_VASc")
        result.content should include("score CHADS2")
        result.content should include("cha2ds2_vasc_total [5 _]")
        result.content should include("cha2ds2_vasc_risk_band [high _]")
        result.content should include("chads2_total [3 _]")
        result.content should include("chads2_risk_band [high _]")
        result.content should include("review_documented_stroke_risk_factors")
        result.warnings shouldBe Nil
      }
    }
  }
}
