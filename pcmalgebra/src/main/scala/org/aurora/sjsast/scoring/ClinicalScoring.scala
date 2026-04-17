package org.aurora.sjsast.scoring

import org.aurora.sjsast.{ClinicalValue, IssueCoordinate, PCM}
import org.aurora.sjsast.scoring.af.AfScorer
import org.aurora.sjsast.scoring.gcs.GcsScorer

import scala.collection.mutable

object ClinicalScoring:
  def apply(pcm: PCM): ScoreUpdate =
    val facts = ClinicalFacts.from(pcm)
    val scoreValues = mutable.ListBuffer.empty[ClinicalValue]
    val derivedIssues = mutable.ListBuffer.empty[IssueCoordinate]
    val derivedModuleImports = mutable.LinkedHashMap.empty[String, String]

    GcsScorer.compute(facts).foreach { result =>
      result.total.foreach(total => scoreValues += ScoreWriteback.intScore("gcs_total", total))
      result.source.foreach(source => scoreValues += ScoreWriteback.textScore("gcs_total_source", source))
      result.severity.foreach(severity => scoreValues += ScoreWriteback.textScore("gcs_severity", severity))
      result.status.foreach(status => scoreValues += ScoreWriteback.textScore("gcs_status", status))
      appendDerivedIssue(result.derivedIssue, derivedIssues, derivedModuleImports)
    }

    AfScorer.compute(facts).foreach { result =>
      result.total.foreach(total => scoreValues += ScoreWriteback.intScore("cha2ds2_vasc_total", total))
      result.riskBand.foreach(band => scoreValues += ScoreWriteback.textScore("cha2ds2_vasc_risk_band", band))
      result.status.foreach(status => scoreValues += ScoreWriteback.textScore("cha2ds2_vasc_status", status))
      appendDerivedIssue(result.derivedIssue, derivedIssues, derivedModuleImports)
    }

    ScoreUpdate(
      pcm = ScoreWriteback(pcm, scoreValues.toList, derivedIssues.toList),
      derivedModuleImports = derivedModuleImports.toMap
    )

  private def appendDerivedIssue(
      derivedIssue: Option[IssueCoordinate],
      derivedIssues: mutable.ListBuffer[IssueCoordinate],
      derivedModuleImports: mutable.LinkedHashMap[String, String]
  ): Unit =
    derivedIssue.foreach { issue =>
      derivedIssues += issue
      issue.fromMods.headOption.foreach(module => derivedModuleImports += (module -> issue.name))
    }
