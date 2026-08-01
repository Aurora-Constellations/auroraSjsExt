package org.aurora.sjsast.utils

import org.aurora.sjsast.QU

enum Qualifier(val elkType: String):
  case Draft extends Qualifier("Draft")
  case Urgent extends Qualifier("Urgent")
  case Negative extends Qualifier("Negative")
  case Normal extends Qualifier("Normal")

object Qualifier:
  def fromQu(quNode: QU): Qualifier =
    val chars = quNode.query
    if (chars.contains('~')) Negative
    else if (chars.contains('!')) Urgent
    else if (chars.contains('?')) Draft
    else Normal