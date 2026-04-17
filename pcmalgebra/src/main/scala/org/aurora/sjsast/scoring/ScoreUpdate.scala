package org.aurora.sjsast.scoring

import org.aurora.sjsast.PCM

final case class ScoreUpdate(
    pcm: PCM,
    derivedModuleImports: Map[String, String] = Map.empty
)
