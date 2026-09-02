package org.aurora.sjsast.scoring.summary

import org.aurora.sjsast.{IntValue, PCM, ParametricModeling, StringValue}
import org.aurora.sjsast.scoring.{ClinicalScoring, ScoreWriteback}

final case class GcsAdultSummary(
    status: String,
    gcs_total: Option[Int] = None,
    gcs_total_source: Option[String] = None,
    gcs_severity: Option[String] = None
)

final case class Cha2Ds2VascSummary(
    status: String,
    cha2ds2_vasc_total: Option[Int] = None,
    cha2ds2_vasc_risk_band: Option[String] = None
)

final case class ScoreSummary(
    GCS_Adult: GcsAdultSummary,
    CHA2DS2_VASc: Cha2Ds2VascSummary
)

object ScoreSummary:
  /** Applies the standard age normalization and runs a fresh scoring pass. */
  def fromPcm(pcm: PCM): ScoreSummary =
    fromScoredPcm(ClinicalScoring(ParametricModeling.applyAgeConstraint(pcm)).pcm)

  /** Summarizes score fields already present in a PCM without recomputing them. */
  def fromScoredPcm(pcm: PCM): ScoreSummary =
    val gcsTotal = intScoreValue(pcm, "gcs_total")
    val gcs = GcsAdultSummary(
      status = textScoreValue(pcm, "gcs_status")
        .orElse(gcsTotal.map(_ => "computable"))
        .getOrElse("not_present"),
      gcs_total = gcsTotal,
      gcs_total_source = textScoreValue(pcm, "gcs_total_source"),
      gcs_severity = textScoreValue(pcm, "gcs_severity")
    )

    val cha2ds2Total = intScoreValue(pcm, "cha2ds2_vasc_total")
    val cha2ds2 = Cha2Ds2VascSummary(
      status = textScoreValue(pcm, "cha2ds2_vasc_status")
        .orElse(cha2ds2Total.map(_ => "computable"))
        .getOrElse("not_present"),
      cha2ds2_vasc_total = cha2ds2Total,
      cha2ds2_vasc_risk_band = textScoreValue(pcm, "cha2ds2_vasc_risk_band")
    )

    ScoreSummary(GCS_Adult = gcs, CHA2DS2_VASc = cha2ds2)

  private def intScoreValue(pcm: PCM, name: String): Option[Int] =
    ScoreWriteback.scoreValue(pcm, name).collect { case IntValue(value) => value }

  private def textScoreValue(pcm: PCM, name: String): Option[String] =
    ScoreWriteback.scoreValue(pcm, name).collect { case StringValue(value) => value }
