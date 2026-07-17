package org.aurora

import org.aurora.sjsast.QU

enum EdgeQualifier(val elkType: String):
  case Draft extends EdgeQualifier("DraftEdge")
  case Urgent extends EdgeQualifier("UrgentEdge")
  case Negative extends EdgeQualifier("NegativeEdge")
  case Normal extends EdgeQualifier("NormalEdge")

object EdgeQualifier:
  def fromQu(quNode: QU): EdgeQualifier =
    val chars = quNode.query
    if (chars.contains('~')) Negative
    else if (chars.contains('!')) Urgent
    else if (chars.contains('?')) Draft
    else Normal