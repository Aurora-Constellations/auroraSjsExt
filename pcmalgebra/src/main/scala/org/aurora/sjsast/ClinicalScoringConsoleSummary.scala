package org.aurora.sjsast

object ClinicalScoringConsoleSummary:
  type GcsAdultSummary = org.aurora.sjsast.scoring.summary.GcsAdultSummary
  type Cha2Ds2VascSummary = org.aurora.sjsast.scoring.summary.Cha2Ds2VascSummary
  type ScoreSummary = org.aurora.sjsast.scoring.summary.ScoreSummary

  def fromPcm(pcm: PCM): ScoreSummary =
    org.aurora.sjsast.scoring.summary.ScoreSummary.fromPcm(pcm)
