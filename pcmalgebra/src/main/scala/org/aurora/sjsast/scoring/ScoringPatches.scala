package org.aurora.sjsast.scoring

import org.aurora.sjsast.JoinMeet.*
import org.aurora.sjsast.*

object ScoringPatches:

  def applyDerivedScores(pcm: PCM): PCM =
    val facts = ClinicalFacts.from(pcm)
    val scorePatch = gcsScorePatch(facts)

    pcm |+| scorePatch

  private def gcsScorePatch(facts: ClinicalFacts): PCM =
    // todo: build the real GCS score patch here 
    PCM()

  // todo: add AF score output here
  // todo: add GCS total/severity/source/status here
