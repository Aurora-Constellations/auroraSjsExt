package org.aurora.sjsast.utils

enum NarrativeType(val prefix: String, val elkType: String):
  case Normal extends NarrativeType("--", "NormalNarrative")
  case Urgent extends NarrativeType("!!", "UrgentNarrative")
  case Draft extends NarrativeType("??", "DraftNarrative")
  case UrgentCompleted extends NarrativeType("xx", "UrgentCompletedNarrative")
  case DraftCompleted extends NarrativeType("..", "DraftCompletedNarrative")

object NarrativeType:
  // Helper to safely parse the correct Enum based on the string prefix
  def fromStatement(name: String): NarrativeType =
    NarrativeType.values
      .find(t => name.startsWith(t.prefix))
      .getOrElse(Normal)