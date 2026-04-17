package org.aurora.sjsast

object ClinicalScoring:
  type ScoreUpdate = org.aurora.sjsast.scoring.ScoreUpdate

  def apply(pcm: PCM): ScoreUpdate =
    org.aurora.sjsast.scoring.ClinicalScoring(pcm)
