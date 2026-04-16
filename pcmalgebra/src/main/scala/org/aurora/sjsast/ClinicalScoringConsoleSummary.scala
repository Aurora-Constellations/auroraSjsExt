package org.aurora.sjsast

object ClinicalScoringConsoleSummary:

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

  def fromPcm(pcm: PCM): ScoreSummary =
    val scored = ClinicalScoring(ParametricModeling.applyAgeConstraint(pcm))

    val gcsTotal = intScoreValue(scored.pcm, "gcs_total")
    val gcsStatus = textScoreValue(scored.pcm, "gcs_status")
    val gcs = GcsAdultSummary(
      status = gcsStatus.orElse(gcsTotal.map(_ => "computable")).getOrElse("not_present"),
      gcs_total = gcsTotal,
      gcs_total_source = textScoreValue(scored.pcm, "gcs_total_source"),
      gcs_severity = textScoreValue(scored.pcm, "gcs_severity")
    )

    val cha2ds2Total = intScoreValue(scored.pcm, "cha2ds2_vasc_total")
    val cha2ds2Status = textScoreValue(scored.pcm, "cha2ds2_vasc_status")
    val cha2ds2 = Cha2Ds2VascSummary(
      status = cha2ds2Status.orElse(cha2ds2Total.map(_ => "computable")).getOrElse("not_present"),
      cha2ds2_vasc_total = cha2ds2Total,
      cha2ds2_vasc_risk_band = textScoreValue(scored.pcm, "cha2ds2_vasc_risk_band")
    )

    ScoreSummary(
      GCS_Adult = gcs,
      CHA2DS2_VASc = cha2ds2
    )

  private def intScoreValue(pcm: PCM, name: String): Option[Int] =
    scoreValue(pcm, name).collect { case IntValue(value) => value }

  private def textScoreValue(pcm: PCM, name: String): Option[String] =
    scoreValue(pcm, name).collect { case StringValue(value) => value }

  private def scoreValue(pcm: PCM, name: String): Option[Value] =
    pcm.cio
      .get("Clinical")
      .collect { case clinical: Clinical => clinical }
      .flatMap(_.ngc.find(_.name == "Scores:"))
      .flatMap(_.coordinates.collectFirst {
        case value: ClinicalValue if value.name == name && value.values.nonEmpty => value.values.head.value
      })
