package org.aurora.sjsast.scoring

import org.aurora.sjsast.JoinMeet.*
import org.aurora.sjsast.*
import org.aurora.sjsast.scoring.af.AfScorer

object ScoringPatches:

  def applyDerivedScores(pcm: PCM): PCM =
    val facts = ClinicalFacts.from(pcm)
    val scorePatch = afScorePatch(facts)

    pcm |+| scorePatch


  private def afScorePatch(facts: ClinicalFacts): PCM =
    AfScorer.compute(facts) match
      case Some(result) => scoresPcm(afValues(result))
      case None => PCM()



  private def afValues(result: AfScorer.Result): List[ClinicalValue] =
    List(
      result.total.map(value => intScore("cha2ds2_vasc_total", value)),
      result.riskBand.map(value => stringScore("cha2ds2_vasc_risk_band", value.outputValue)),
      result.status.map(value => stringScore("cha2ds2_vasc_status", value.outputValue))
    ).flatten

  private def scoresPcm(values: List[ClinicalValue]): PCM =
    if values.isEmpty then PCM()
    else
      PCM(
        LHMap(
          "Clinical" -> Clinical(
            ngc = LHSet(
              NGC(
                name = ScoringConstants.ScoreGroupName,
                coordinates = LHSet.from(values)
              )
            )
          )
        )
      )

  private def intScore(name: String, value: Int): ClinicalValue =
    ClinicalValue(
      name = name,
      values = List(SingleValueUnit(IntValue(value), ScoringConstants.PlaceholderUnit))
    )

  private def stringScore(name: String, value: String): ClinicalValue =
    ClinicalValue(
      name = name,
      values = List(SingleValueUnit(StringValue(value), ScoringConstants.PlaceholderUnit))
    )






  private def gcsScorePatch(facts: ClinicalFacts): PCM =
    // todo: build the real GCS score patch here 
    PCM()

  // todo: add AF score output here
  // todo: add GCS total/severity/source/status here
