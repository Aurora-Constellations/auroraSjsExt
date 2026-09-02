package org.aurora.sjsast.scoring

import org.aurora.sjsast.PCM

/** The result of one deterministic scoring pass.
  *
  * `pcm` contains refreshed derived score fields and issues. Module imports are
  * returned separately so callers can decide whether and how to resolve them.
  */
final case class ScoreUpdate(
    pcm: PCM,
    derivedModuleImports: Map[String, String] = Map.empty
)
