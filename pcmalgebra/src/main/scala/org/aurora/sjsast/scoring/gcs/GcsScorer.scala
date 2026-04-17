package org.aurora.sjsast.scoring.gcs

import org.aurora.sjsast.IssueCoordinate
import org.aurora.sjsast.scoring.{ClinicalFacts, Parsing}

object GcsScorer:
  final case class Result(
      total: Option[Int],
      severity: Option[String],
      source: Option[String],
      status: Option[String],
      derivedIssue: Option[IssueCoordinate]
  )

  private final case class Component(score: Option[Int], notTestable: Boolean = false)

  private val EyeKeys = List("gcs_eye", "glasgow_eye", "gcs_eye_response", "glasgow_coma_scale_eye")
  private val VerbalKeys = List("gcs_verbal", "glasgow_verbal", "gcs_verbal_response", "glasgow_coma_scale_verbal")
  private val MotorKeys = List("gcs_motor", "glasgow_motor", "gcs_motor_response", "glasgow_coma_scale_motor")
  private val TotalKeys = List("gcs_total", "glasgow_total", "glasgow_coma_scale_total")

  def compute(facts: ClinicalFacts): Option[Result] =
    val eye = resolveComponent(facts, EyeKeys, max = 4, synonyms = Map(
      "spontaneous" -> 4,
      "opens_spontaneously" -> 4,
      "to_sound" -> 3,
      "to_voice" -> 3,
      "to_verbal_command" -> 3,
      "verbal_command" -> 3,
      "voice" -> 3,
      "to_pressure" -> 2,
      "to_pain" -> 2,
      "pain" -> 2,
      "none" -> 1
    ))
    val verbal = resolveComponent(facts, VerbalKeys, max = 5, synonyms = Map(
      "orientated" -> 5,
      "oriented" -> 5,
      "confused" -> 4,
      "words" -> 3,
      "inappropriate_words" -> 3,
      "sounds" -> 2,
      "incomprehensible_sounds" -> 2,
      "none" -> 1
    ))
    val motor = resolveComponent(facts, MotorKeys, max = 6, synonyms = Map(
      "obeys" -> 6,
      "obeys_commands" -> 6,
      "localising" -> 5,
      "localizing" -> 5,
      "localises" -> 5,
      "localizes" -> 5,
      "normal_flexion" -> 4,
      "withdrawal_from_pain" -> 4,
      "withdraws" -> 4,
      "abnormal_flexion" -> 3,
      "flexion" -> 3,
      "extension" -> 2,
      "none" -> 1
    ))
    val manualTotal = facts.firstValue(TotalKeys).flatMap(Parsing.parseInt)

    val hasAnyGcsData =
      eye.score.nonEmpty || verbal.score.nonEmpty || motor.score.nonEmpty || manualTotal.nonEmpty ||
        eye.notTestable || verbal.notTestable || motor.notTestable

    if !hasAnyGcsData then None
    else
      val hasNotTestable = List(eye, verbal, motor).exists(_.notTestable)
      val componentScores = List(eye.score, verbal.score, motor.score)
      val componentTotal = if componentScores.forall(_.nonEmpty) then Some(componentScores.flatten.sum) else None
      val total = if hasNotTestable then None else componentTotal.orElse(manualTotal)
      val source = if hasNotTestable then None else total.map(_ => if componentTotal.nonEmpty then "derived" else "manual")
      val status =
        if hasNotTestable then Some("not_testable")
        else if total.isEmpty then Some("incomplete")
        else None
      val severity = total.map(severityFor)
      val derivedIssue = severity.flatMap {
        case "severe"   => Some(IssueCoordinate(name = "score_gcs_severe", fromMods = List("gcs_severe")))
        case "moderate" => Some(IssueCoordinate(name = "score_gcs_moderate", fromMods = List("gcs_moderate")))
        case _          => None
      }

      Some(Result(total, severity, source, status, derivedIssue))

  private def resolveComponent(
      facts: ClinicalFacts,
      keys: List[String],
      max: Int,
      synonyms: Map[String, Int]
  ): Component =
    facts.firstValue(keys) match
      case None => Component(None)
      case Some(value) =>
        Parsing.asText(value) match
          case Some(text) if Set("nt", "not_testable").contains(text) => Component(None, notTestable = true)
          case Some(text) if synonyms.contains(text) => Component(Some(synonyms(text)))
          case _ =>
            Parsing.parseInt(value).filter(score => score >= 1 && score <= max) match
              case Some(score) => Component(Some(score))
              case None        => Component(None)

  private def severityFor(total: Int): String =
    if total <= 8 then "severe"
    else if total <= 12 then "moderate"
    else "mild"
