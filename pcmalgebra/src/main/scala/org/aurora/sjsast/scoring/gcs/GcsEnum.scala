package org.aurora.sjsast.scoring.gcs

trait GcsComponent:
  def score: Int
  def description: String


enum ComponentResolution[T]:
  case Missing
  case NotTestable
  case Resolved(value: T)


enum Eye(val score: Int, val description: String) extends GcsComponent:
  case Spontaneous extends Eye(4, "eyes open spontaneously")
  case ToVoice extends Eye(3, "eyes open to voice or verbal command")
  case ToPain extends Eye(2, "eyes open to pain or pressure")
  case None extends Eye(1, "no eye opening")


enum Verbal(val score: Int, val description: String) extends GcsComponent:
  case Oriented extends Verbal(5, "oriented verbal response")
  case Confused extends Verbal(4, "confused verbal response")
  case Words extends Verbal(3, "inappropriate words")
  case Sounds extends Verbal(2, "incomprehensible sounds")
  case None extends Verbal(1, "no verbal response")


enum Motor(val score: Int, val description: String) extends GcsComponent:
  case ObeysCommands extends Motor(6, "obeys commands")
  case LocalizesPain extends Motor(5, "localizes pain")
  case WithdrawsFromPain extends Motor(4, "withdraws from pain")
  case AbnormalFlexion extends Motor(3, "abnormal flexion")
  case Extension extends Motor(2, "extension response")
  case None extends Motor(1, "no motor response")


enum GcsSeverity(val outputValue: String):
  case Severe extends GcsSeverity("severe")
  case Moderate extends GcsSeverity("moderate")
  case Mild extends GcsSeverity("mild")


enum GcsStatus(val outputValue: String):
  case NotTestable extends GcsStatus("not_testable")
  case Incomplete extends GcsStatus("incomplete")


enum GcsTotalSource(val outputValue: String):
  case Derived extends GcsTotalSource("derived")
  case Manual extends GcsTotalSource("manual")
