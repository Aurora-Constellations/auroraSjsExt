package org.aurora.sjsast.scoring.gcs

import org.aurora.sjsast.scoring.{ClinicalFacts, Parsing}

object GcsScorer:
  final case class Result(
      total: Option[Int],
      severity: Option[GcsSeverity],
      source: Option[GcsTotalSource],
      status: Option[GcsStatus]
  )

  private[scoring] val EyeKeys = List("gcs_eye", "glasgow_eye", "gcs_eye_response", "glasgow_coma_scale_eye")
  private[scoring] val VerbalKeys = List("gcs_verbal", "glasgow_verbal", "gcs_verbal_response", "glasgow_coma_scale_verbal")
  private[scoring] val MotorKeys = List("gcs_motor", "glasgow_motor", "gcs_motor_response", "glasgow_coma_scale_motor")
  private[scoring] val TotalKeys = List("gcs_total", "glasgow_total", "glasgow_coma_scale_total")

  def compute(facts: ClinicalFacts): Option[Result] =
    val eye = resolveEye(facts)
    val verbal = resolveVerbal(facts)
    val motor = resolveMotor(facts)
    val manualTotal = facts.firstValue(TotalKeys).flatMap(Parsing.parseInt).filter(isValidTotal)

    val componentResolutions = List(eye, verbal, motor)
    val hasAnyGcsData =
      componentResolutions.exists(_ != ComponentResolution.Missing) || manualTotal.nonEmpty

    if !hasAnyGcsData then None
    else if componentResolutions.exists(_ == ComponentResolution.NotTestable) then
      Some(Result(total = None, severity = None, source = None, status = Some(GcsStatus.NotTestable)))
    else
      val componentTotal = derivedComponentTotal(eye, verbal, motor)
      val total = componentTotal.orElse(manualTotal)
      val source = total.map(_ => if componentTotal.nonEmpty then GcsTotalSource.Derived else GcsTotalSource.Manual)
      val status = if total.isEmpty then Some(GcsStatus.Incomplete) else None
      val severity = total.flatMap(GcsSeverity.fromTotal)

      Some(Result(total = total, severity = severity, source = source, status = status))

  private[scoring] def resolveEye(facts: ClinicalFacts): ComponentResolution[Eye] =
    facts.firstValue(EyeKeys)
      .flatMap(Parsing.asText)
      .map(Eye.resolveInput)
      .getOrElse(ComponentResolution.Missing)

  private[scoring] def resolveVerbal(facts: ClinicalFacts): ComponentResolution[Verbal] =
    facts.firstValue(VerbalKeys)
      .flatMap(Parsing.asText)
      .map(Verbal.resolveInput)
      .getOrElse(ComponentResolution.Missing)

  private[scoring] def resolveMotor(facts: ClinicalFacts): ComponentResolution[Motor] =
    facts.firstValue(MotorKeys)
      .flatMap(Parsing.asText)
      .map(Motor.resolveInput)
      .getOrElse(ComponentResolution.Missing)

  private def derivedComponentTotal(
      eye: ComponentResolution[Eye],
      verbal: ComponentResolution[Verbal],
      motor: ComponentResolution[Motor]
  ): Option[Int] =
    (eye, verbal, motor) match
      case (
            ComponentResolution.Resolved(resolvedEye),
            ComponentResolution.Resolved(resolvedVerbal),
            ComponentResolution.Resolved(resolvedMotor)
          ) =>
        Some(resolvedEye.score + resolvedVerbal.score + resolvedMotor.score)
      case _ =>
        None

  private def isValidTotal(total: Int): Boolean =
    GcsSeverity.fromTotal(total).nonEmpty
