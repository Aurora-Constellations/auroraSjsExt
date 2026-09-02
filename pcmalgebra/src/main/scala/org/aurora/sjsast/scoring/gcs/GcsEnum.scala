package org.aurora.sjsast.scoring.gcs

trait GcsComponent:
  def score: Int
  def description: String


private def findGcsComponentByScore[A <: GcsComponent](values: Array[A], inputScore: Int): Option[A] =
  var result: Option[A] = Option.empty

  for component <- values
    if component.score == inputScore
    do result = Some(component)
  result


private def normalizeGcsParserInput(input: String): String =
  input.trim.toLowerCase.replace(" ", "_").replace("-", "_")


enum ComponentResolution[+T]:
  case Missing
  case NotTestable
  case Resolved(value: T)


private def resolveGcsComponentInput[A <: GcsComponent](
    input: String,
    findByScore: Int => Option[A],
    parse: String => Option[A]
): ComponentResolution[A] =
  normalizeGcsParserInput(input) match
    case "nt" | "not_testable" => ComponentResolution.NotTestable
    case normalized =>
      normalized.toIntOption
        .flatMap(findByScore)
        .orElse(parse(normalized))
        .map(ComponentResolution.Resolved(_))
        .getOrElse(ComponentResolution.Missing)


enum Eye(val score: Int, val description: String) extends GcsComponent:
  case Spontaneous extends Eye(4, "eyes open spontaneously")
  case ToVoice extends Eye(3, "eyes open to voice or verbal command")
  case ToPain extends Eye(2, "eyes open to pain or pressure")
  case None extends Eye(1, "no eye opening")

object Eye:
  def findByScore(inputScore: Int): Option[Eye] =
    findGcsComponentByScore(Eye.values, inputScore)

  def parse(input: String): Option[Eye] =
    normalizeGcsParserInput(input) match
      case "spontaneous" | "opens_spontaneously" => Some(Eye.Spontaneous)
      case "to_sound" | "to_voice" | "to_verbal_command" | "verbal_command" | "voice" =>
        Some(Eye.ToVoice)
      case "to_pain" | "to_pressure" | "pain" => Some(Eye.ToPain)
      case "none" | "no_eye_opening" => Some(Eye.None)
      case _ => Option.empty

  def resolveInput(input: String): ComponentResolution[Eye] =
    resolveGcsComponentInput(input, Eye.findByScore, Eye.parse)

enum Verbal(val score: Int, val description: String) extends GcsComponent:
  case Oriented extends Verbal(5, "oriented verbal response")
  case Confused extends Verbal(4, "confused verbal response")
  case Words extends Verbal(3, "inappropriate words")
  case Sounds extends Verbal(2, "incomprehensible sounds")
  case None extends Verbal(1, "no verbal response")

object Verbal:
  def findByScore(inputScore: Int): Option[Verbal] =
    findGcsComponentByScore(Verbal.values, inputScore)

  def parse(input: String): Option[Verbal] =
    normalizeGcsParserInput(input) match
      case "orientated" | "oriented" => Some(Verbal.Oriented)
      case "confused" => Some(Verbal.Confused)
      case "words" | "inappropriate_words" => Some(Verbal.Words)
      case "sounds" | "incomprehensible_sounds" => Some(Verbal.Sounds)
      case "none" | "no_verbal_response" => Some(Verbal.None)
      case _ => Option.empty

  def resolveInput(input: String): ComponentResolution[Verbal] =
    resolveGcsComponentInput(input, Verbal.findByScore, Verbal.parse)


enum Motor(val score: Int, val description: String) extends GcsComponent:
  case ObeysCommands extends Motor(6, "obeys commands")
  case LocalizesPain extends Motor(5, "localizes pain")
  case WithdrawsFromPain extends Motor(4, "withdraws from pain")
  case AbnormalFlexion extends Motor(3, "abnormal flexion")
  case Extension extends Motor(2, "extension response")
  case None extends Motor(1, "no motor response")

object Motor:
  def findByScore(inputScore: Int): Option[Motor] =
    findGcsComponentByScore(Motor.values, inputScore)

  def parse(input: String): Option[Motor] =
    normalizeGcsParserInput(input) match
      case "obeys_commands" | "obeys" => Some(Motor.ObeysCommands)
      case "localises" | "localising" | "localizes" | "localizes_pain" | "localizing" =>
        Some(Motor.LocalizesPain)
      case "normal_flexion" | "withdrawal_from_pain" | "withdraws_from_pain" | "withdraws" =>
        Some(Motor.WithdrawsFromPain)
      case "abnormal_flexion" | "flexion" => Some(Motor.AbnormalFlexion)
      case "extension" => Some(Motor.Extension)
      case "none" | "no_motor_response" => Some(Motor.None)
      case _ => Option.empty

  def resolveInput(input: String): ComponentResolution[Motor] =
    resolveGcsComponentInput(input, Motor.findByScore, Motor.parse)


enum GcsSeverity(val outputValue: String):
  case Severe extends GcsSeverity("severe")
  case Moderate extends GcsSeverity("moderate")
  case Mild extends GcsSeverity("mild")

object GcsSeverity:
  def fromTotal(total: Int): Option[GcsSeverity] =
    if total < 3 || total > 15 then None
    else if total <= 8 then Some(GcsSeverity.Severe)
    else if total <= 12 then Some(GcsSeverity.Moderate)
    else Some(GcsSeverity.Mild)

  def fromOutputValue(input: String): Option[GcsSeverity] =
    input.trim.toLowerCase match
      case "severe" => Some(GcsSeverity.Severe)
      case "moderate" => Some(GcsSeverity.Moderate)
      case "mild" => Some(GcsSeverity.Mild)
      case _ => None


enum GcsStatus(val outputValue: String):
  case NotTestable extends GcsStatus("not_testable")
  case Incomplete extends GcsStatus("incomplete")

object GcsStatus:
  def fromOutputValue(input: String): Option[GcsStatus] =
    input.trim.toLowerCase match
      case "not_testable" | "not testable" => Some(GcsStatus.NotTestable)
      case "incomplete" => Some(GcsStatus.Incomplete)
      case _ => None


enum GcsTotalSource(val outputValue: String):
  case Derived extends GcsTotalSource("derived")
  case Manual extends GcsTotalSource("manual")

object GcsTotalSource:
  def fromOutputValue(input: String): Option[GcsTotalSource] =
    input.trim.toLowerCase match
      case "derived" => Some(GcsTotalSource.Derived)
      case "manual" => Some(GcsTotalSource.Manual)
      case _ => None
