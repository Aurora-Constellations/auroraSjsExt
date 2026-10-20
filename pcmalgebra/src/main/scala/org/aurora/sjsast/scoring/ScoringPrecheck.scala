package org.aurora.sjsast.scoring

import org.aurora.sjsast.{IncompleteValue, PCM, SingleValueUnit, StringValue}
import org.aurora.sjsast.scoring.af.AfScorer
import org.aurora.sjsast.scoring.gcs.{ComponentResolution, GcsScorer, GcsSeverity}

/** A read-only explanation of whether the supported scores have usable inputs. */
enum ScoringPrecheckStatus(val label: String):
  case NotApplicable extends ScoringPrecheckStatus("not applicable")
  case Ready extends ScoringPrecheckStatus("ready")
  case Incomplete extends ScoringPrecheckStatus("incomplete")
  case NotTestable extends ScoringPrecheckStatus("not testable")

enum ScoringInputProblem(val label: String):
  case Missing extends ScoringInputProblem("missing")
  case Invalid extends ScoringInputProblem("invalid")

final case class ScoringInputIssue(
    field: String,
    problem: ScoringInputProblem,
    expected: String
)

final case class ScoringScoreCheck(
    score: String,
    status: ScoringPrecheckStatus,
    issues: List[ScoringInputIssue] = Nil
)

final case class ScoringPrecheckReport(
    gcs: ScoringScoreCheck,
    cha2ds2Vasc: ScoringScoreCheck
):
  def checks: List[ScoringScoreCheck] = List(gcs, cha2ds2Vasc)
  def isValid: Boolean = checks.forall(_.issues.isEmpty)

object ScoringPrecheck:
  /** Reports missing and invalid source facts without changing the PCM. */
  def validate(pcm: PCM): ScoringPrecheckReport =
    val facts = ClinicalFacts.from(pcm)
    ScoringPrecheckReport(checkGcs(facts), checkAf(facts))

  private def checkGcs(facts: ClinicalFacts): ScoringScoreCheck =
    val score = "GCS Adult"
    val componentKeys = GcsScorer.EyeKeys ++ GcsScorer.VerbalKeys ++ GcsScorer.MotorKeys
    val allKeys = componentKeys ++ GcsScorer.TotalKeys
    if !allKeys.exists(facts.values.contains) then
      ScoringScoreCheck(score, ScoringPrecheckStatus.NotApplicable)
    else
      val eye = GcsScorer.resolveEye(facts)
      val verbal = GcsScorer.resolveVerbal(facts)
      val motor = GcsScorer.resolveMotor(facts)
      val notTestable = List(eye, verbal, motor).exists(_ == ComponentResolution.NotTestable)
      if notTestable then
        ScoringScoreCheck(score, ScoringPrecheckStatus.NotTestable)
      else
        val hasComponentTotal =
          eye.isInstanceOf[ComponentResolution.Resolved[?]] &&
            verbal.isInstanceOf[ComponentResolution.Resolved[?]] &&
            motor.isInstanceOf[ComponentResolution.Resolved[?]]
        val manualTotal = facts.firstValue(GcsScorer.TotalKeys)
          .flatMap(Parsing.parseInt)
          .filter(total => GcsSeverity.fromTotal(total).nonEmpty)
        if hasComponentTotal || manualTotal.nonEmpty then
          ScoringScoreCheck(score, ScoringPrecheckStatus.Ready)
        else
          val componentIssues = List(
            componentIssue(facts, GcsScorer.EyeKeys, "gcs_eye", eye, "GCS eye response (1–4 or a supported label)"),
            componentIssue(facts, GcsScorer.VerbalKeys, "gcs_verbal", verbal, "GCS verbal response (1–5 or a supported label)"),
            componentIssue(facts, GcsScorer.MotorKeys, "gcs_motor", motor, "GCS motor response (1–6 or a supported label)")
          ).flatten
          val manualIssue =
            if GcsScorer.TotalKeys.exists(facts.values.contains) then
              Some(inputIssue(facts, GcsScorer.TotalKeys, "gcs_total", "manual GCS total from 3 to 15"))
            else None
          ScoringScoreCheck(score, ScoringPrecheckStatus.Incomplete, componentIssues ++ manualIssue)

  private def componentIssue[T](
      facts: ClinicalFacts,
      keys: List[String],
      field: String,
      resolution: ComponentResolution[T],
      expected: String
  ): Option[ScoringInputIssue] =
    resolution match
      case ComponentResolution.Resolved(_) | ComponentResolution.NotTestable => None
      case ComponentResolution.Missing => Some(inputIssue(facts, keys, field, expected))

  private def checkAf(facts: ClinicalFacts): ScoringScoreCheck =
    val score = "CHA2DS2-VASc"
    AfScorer.resolveBoolean(facts, AfScorer.DiagnosisKeys, AfScorer.DiagnosisIssueNames) match
      case Some(false) =>
        ScoringScoreCheck(score, ScoringPrecheckStatus.NotApplicable)
      case None if !AfScorer.DiagnosisKeys.exists(facts.values.contains) =>
        ScoringScoreCheck(score, ScoringPrecheckStatus.NotApplicable)
      case None =>
        ScoringScoreCheck(
          score,
          ScoringPrecheckStatus.Incomplete,
          List(inputIssue(facts, AfScorer.DiagnosisKeys, "atrial_fibrillation", "confirmed or absent AF diagnosis"))
        )
      case Some(true) =>
        val issues = List(
          requireInput(facts, AfScorer.AgeKeys, "age", "nonnegative age in years", AfScorer.resolveAge(facts).nonEmpty),
          requireInput(facts, AfScorer.SexKeys, "sex", "female or male sex category", AfScorer.resolveSex(facts).nonEmpty),
          requireBoolean(facts, AfScorer.HeartFailureKeys, AfScorer.HeartFailureIssueNames, "heart_failure"),
          requireBoolean(facts, AfScorer.HypertensionKeys, AfScorer.HypertensionIssueNames, "hypertension"),
          requireBoolean(facts, AfScorer.DiabetesKeys, AfScorer.DiabetesIssueNames, "diabetes"),
          requireBoolean(facts, AfScorer.PriorStrokeKeys, AfScorer.PriorStrokeIssueNames, "prior_stroke_tia_te"),
          requireBoolean(facts, AfScorer.VascularDiseaseKeys, AfScorer.VascularDiseaseIssueNames, "vascular_disease")
        ).flatten
        val status = if issues.isEmpty then ScoringPrecheckStatus.Ready else ScoringPrecheckStatus.Incomplete
        ScoringScoreCheck(score, status, issues)

  private def requireBoolean(
      facts: ClinicalFacts,
      keys: List[String],
      issueNames: Set[String],
      field: String
  ): Option[ScoringInputIssue] =
    requireInput(
      facts,
      keys,
      field,
      "present or absent (a matching active issue also means present)",
      AfScorer.resolveBoolean(facts, keys, issueNames).nonEmpty
    )

  private def requireInput(
      facts: ClinicalFacts,
      keys: List[String],
      field: String,
      expected: String,
      resolved: Boolean
  ): Option[ScoringInputIssue] =
    if resolved then None else Some(inputIssue(facts, keys, field, expected))

  private def inputIssue(
      facts: ClinicalFacts,
      keys: List[String],
      field: String,
      expected: String
  ): ScoringInputIssue =
    val problem =
      facts.firstValue(keys) match
        case None => ScoringInputProblem.Missing
        case Some(value) if isPlaceholder(value) => ScoringInputProblem.Missing
        case Some(_) => ScoringInputProblem.Invalid
    ScoringInputIssue(field, problem, expected)

  private def isPlaceholder(value: SingleValueUnit): Boolean =
    value.value match
      case IncompleteValue => true
      case StringValue(text) => text.trim == "???"
      case _ => false
