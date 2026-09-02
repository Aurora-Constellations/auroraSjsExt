package org.aurora.sjsast.scoring

import org.aurora.sjsast.{ClinicalItem, IssueCoordinate, PCM}
import org.aurora.sjsast.scoring.af.{AfScorer, Cha2ds2VascRiskBand}
import org.aurora.sjsast.scoring.gcs.{GcsScorer, GcsSeverity}

import scala.collection.mutable

/** Runs every supported clinical scorer and refreshes their derived outputs. */
object ClinicalScoring:
  def apply(pcm: PCM): ScoreUpdate =
    val facts = ClinicalFacts.from(pcm)
    val scoreValues = mutable.ListBuffer.empty[ClinicalItem]
    val derivedIssues = mutable.ListBuffer.empty[IssueCoordinate]
    val derivedModuleImports = mutable.LinkedHashMap.empty[String, String]

    GcsScorer.compute(facts).foreach { result =>
      result.total.foreach(total => scoreValues += ScoreWriteback.intScore("gcs_total", total))
      result.source.foreach(source =>
        scoreValues += ScoreWriteback.textScore("gcs_total_source", source.outputValue)
      )
      result.severity.foreach(severity =>
        scoreValues += ScoreWriteback.textScore("gcs_severity", severity.outputValue)
      )
      result.status.foreach(status =>
        scoreValues += ScoreWriteback.textScore("gcs_status", status.outputValue)
      )
      result.severity.flatMap(gcsDerivedIssue).foreach { issue =>
        appendDerivedIssue(issue, derivedIssues, derivedModuleImports)
      }
    }

    AfScorer.compute(facts).foreach { result =>
      result.total.foreach(total =>
        scoreValues += ScoreWriteback.intScore("cha2ds2_vasc_total", total)
      )
      result.riskBand.foreach(riskBand =>
        scoreValues += ScoreWriteback.textScore(
          "cha2ds2_vasc_risk_band",
          riskBand.outputValue
        )
      )
      result.status.foreach(status =>
        scoreValues += ScoreWriteback.textScore(
          "cha2ds2_vasc_status",
          status.outputValue
        )
      )
      result.riskBand.flatMap(afDerivedIssue).foreach { issue =>
        appendDerivedIssue(issue, derivedIssues, derivedModuleImports)
      }
    }

    ScoreUpdate(
      pcm = ScoreWriteback(pcm, scoreValues.toList, derivedIssues.toList),
      derivedModuleImports = derivedModuleImports.toMap
    )

  private def gcsDerivedIssue(severity: GcsSeverity): Option[IssueCoordinate] =
    severity match
      case GcsSeverity.Severe =>
        Some(IssueCoordinate(name = "score_gcs_severe", fromMods = List("gcs_severe")))
      case GcsSeverity.Moderate =>
        Some(IssueCoordinate(name = "score_gcs_moderate", fromMods = List("gcs_moderate")))
      case GcsSeverity.Mild => None

  private def afDerivedIssue(riskBand: Cha2ds2VascRiskBand): Option[IssueCoordinate] =
    riskBand match
      case Cha2ds2VascRiskBand.High =>
        Some(
          IssueCoordinate(
            name = "score_af_stroke_risk_high",
            fromMods = List("af_stroke_risk_high")
          )
        )
      case Cha2ds2VascRiskBand.Intermediate =>
        Some(
          IssueCoordinate(
            name = "score_af_stroke_risk_intermediate",
            fromMods = List("af_stroke_risk_intermediate")
          )
        )
      case Cha2ds2VascRiskBand.Low => None

  private def appendDerivedIssue(
      issue: IssueCoordinate,
      derivedIssues: mutable.ListBuffer[IssueCoordinate],
      derivedModuleImports: mutable.LinkedHashMap[String, String]
  ): Unit =
    derivedIssues += issue
    issue.fromMods.headOption.foreach(module => derivedModuleImports.update(module, issue.name))
