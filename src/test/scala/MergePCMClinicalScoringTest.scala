package com.axiom.MergePCM

import com.axiom.testutils.FileUtils.*
import org.aurora.sjsast.GenAst
import org.aurora.sjsast.scoring.ScoreModuleResolver
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import typings.auroraLangium.cliMod.parse

import scala.concurrent.ExecutionContext
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

class MergePCMClinicalScoringTest extends AsyncWordSpec with Matchers:
  override implicit def executionContext: ExecutionContext = queue

  private val fixtureRoot = cwd / "src" / "test" / "resources" / "clinical_scoring"

  private def fixture(path: String): String =
    fixtureRoot / path

  private def extensionOptions: ScoreModuleResolver.Options =
    ScoreModuleResolver.Options.forExtensionRoot(cwd)

  "MergePCM.generateOrdersDSLResult" should {
    "calculate GCS and merge bundled guidance into the normal result path" in {
      for
        parsed <- parse(fixture("bundled_fallback/gcs_severe_patient.aurora")).toFuture
        result <- MergePCM.generateOrdersDSLResult(
          parsed.asInstanceOf[GenAst.PCM],
          extensionOptions
        )
      yield
        result.content should include("Scores:")
        result.content should include("gcs_total [7 _]")
        result.content should include("gcs_total_source [derived _]")
        result.content should include("gcs_severity [severe _]")
        result.content should include("document_score_basis")
        result.content should include("baseline_note")
        result.warnings shouldBe Nil
    }

    "prefer a score module next to the current PCM over bundled guidance" in {
      for
        parsed <- parse(fixture("local_override/gcs_severe_patient.aurora")).toFuture
        result <- MergePCM.generateOrdersDSLResult(
          parsed.asInstanceOf[GenAst.PCM],
          extensionOptions
        )
      yield
        result.content should include("gcs_total [7 _]")
        result.content should include("local_override_neuro_escalation")
        result.content should not include "document_score_basis"
        result.warnings shouldBe Nil
    }

    "keep calculated scores and report a warning when guidance is unavailable" in {
      for
        parsed <- parse(fixture("bundled_fallback/gcs_severe_patient.aurora")).toFuture
        result <- MergePCM.generateOrdersDSLResult(
          parsed.asInstanceOf[GenAst.PCM],
          ScoreModuleResolver.Options(bundledRoots = Nil)
        )
      yield
        result.content should include("Scores:")
        result.content should include("gcs_total [7 _]")
        result.content should not include "document_score_basis"
        result.warnings should contain("Unable to resolve score module 'gcs_severe'.")
    }

    "calculate CHA2DS2-VASc and merge high-risk guidance" in {
      for
        parsed <- parse(fixture("af_scores/af_high_risk_patient.aurora")).toFuture
        result <- MergePCM.generateOrdersDSLResult(
          parsed.asInstanceOf[GenAst.PCM],
          extensionOptions
        )
      yield
        result.content should include("Scores:")
        result.content should include("cha2ds2_vasc_total [5 _]")
        result.content should include("cha2ds2_vasc_risk_band [high _]")
        result.content should include("review_documented_stroke_risk_factors")
        result.content should not include "chads2_"
        result.warnings shouldBe Nil
    }
  }

  "MergePCM.generateOrdersDSL" should {
    "preserve the original string-returning API while including scoring" in {
      for
        parsed <- parse(fixture("bundled_fallback/gcs_severe_patient.aurora")).toFuture
        content <- MergePCM.generateOrdersDSL(parsed.asInstanceOf[GenAst.PCM])
      yield
        content should include("gcs_total [7 _]")
        content should include("document_score_basis")
    }
  }
