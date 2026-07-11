package org.aurora.sjsast.scoring

import org.aurora.sjsast.JoinMeet.*
import org.aurora.sjsast.*
import org.aurora.sjsast.scoring.af.AfScorer
import org.aurora.sjsast.scoring.gcs.GcsScorer

object ScoringPatches:

  def applyDerivedScores(pcm: PCM): PCM =
    val facts = ClinicalFacts.from(pcm)
    val gcsPatch = gcsScorePatch(facts)
    val afPatch = afScorePatch(facts)

    pcm |+| gcsPatch |+| afPatch

  private def gcsScorePatch(facts: ClinicalFacts): PCM =
    GcsScorer.compute(facts) match
      case None => PCM()
      case Some(result) =>
        val values: List[ClinicalValue] =
          List(
            result.total.map(t =>
              ClinicalValue(
                name = "gcs_total",
                values = List(SingleValueUnit(IntValue(t), ScoringConstants.PlaceholderUnit))
              )
            ),
            result.severity.map(s =>
              ClinicalValue(
                name = "gcs_severity",
                values = List(SingleValueUnit(StringValue(s.outputValue), ScoringConstants.PlaceholderUnit))
              )
            ),
            result.source.map(s =>
              ClinicalValue(
                name = "gcs_total_source",
                values = List(SingleValueUnit(StringValue(s.outputValue), ScoringConstants.PlaceholderUnit))
              )
            ),
            result.status.map(s =>
              ClinicalValue(
                name = "gcs_status",
                values = List(SingleValueUnit(StringValue(s.outputValue), ScoringConstants.PlaceholderUnit))
              )
            )
          ).flatten

        buildScorePatch(values)

  private def afScorePatch(facts: ClinicalFacts): PCM =
    AfScorer.compute(facts) match
      case None => PCM()
      case Some(result) =>
        val values: List[ClinicalValue] =
          List(
            result.total.map(t =>
              ClinicalValue(
                name = "af_cha2ds2_vasc_total",
                values = List(SingleValueUnit(IntValue(t), ScoringConstants.PlaceholderUnit))
              )
            ),
            result.riskBand.map(b =>
              ClinicalValue(
                name = "af_cha2ds2_vasc_risk_band",
                values = List(SingleValueUnit(StringValue(b.outputValue), ScoringConstants.PlaceholderUnit))
              )
            ),
            result.status.map(s =>
              ClinicalValue(
                name = "af_cha2ds2_vasc_status",
                values = List(SingleValueUnit(StringValue(s.outputValue), ScoringConstants.PlaceholderUnit))
              )
            )
          ).flatten

        buildScorePatch(values)

  private def buildScorePatch(values: List[ClinicalValue]): PCM =
    if values.isEmpty then PCM()
    else
      val scoreGroup = NGC(
        name = ScoringConstants.ScoreGroupName,
        coordinates = LHSet[RefCoordinate](values*)
      )
      val clinical = Clinical(ngc = LHSet(scoreGroup))

      PCM(cio = LHMap[String, CIO]("Clinical" -> clinical))