package org.aurora.sjsast.scoring

import org.aurora.sjsast.PCM

/** Compatibility entry point for callers that only need the refreshed PCM. */
object ScoringPatches:
  def applyDerivedScores(pcm: PCM): PCM =
    ClinicalScoring(pcm).pcm
