package org.aurora.sjsast.scoring.af

import org.aurora.sjsast.scoring.{ClinicalFacts, Parsing}

object AfScorer:
  final case class Result(
      total: Option[Int],
      riskBand: Option[Cha2ds2VascRiskBand],
      status: Option[Cha2ds2VascStatus]
  )

  private[scoring] val AgeKeys = List("age", "age_years", "ageyears")
  private[scoring] val SexKeys = List("sex", "gender", "sex_for_score")
  private[scoring] val DiagnosisKeys = List("atrial_fibrillation", "atrial_flutter", "af")
  private[scoring] val HeartFailureKeys = List("heart_failure", "cha2ds2_vasc_heart_failure")
  private[scoring] val HypertensionKeys = List("hypertension", "cha2ds2_vasc_hypertension")
  private[scoring] val DiabetesKeys = List("diabetes", "cha2ds2_vasc_diabetes")
  private[scoring] val PriorStrokeKeys = List("prior_stroke_tia_te", "prior_stroke_tia_thromboembolism", "prior_stroke", "prior_tia")
  private[scoring] val VascularDiseaseKeys = List("vascular_disease", "cha2ds2_vasc_vascular_disease")

  private[scoring] val DiagnosisIssueNames = Set("atrial_fibrillation", "atrial_flutter", "af", "afib", "a_fib", "a_flutter")
  private[scoring] val HeartFailureIssueNames = Set("heart_failure", "congestive_heart_failure", "chf", "hf")
  private[scoring] val HypertensionIssueNames = Set("hypertension", "htn")
  private[scoring] val DiabetesIssueNames = Set("diabetes", "diabetes_mellitus", "dm")
  private[scoring] val PriorStrokeIssueNames = Set("stroke", "tia", "thromboembolism", "prior_stroke", "prior_tia")
  private[scoring] val VascularDiseaseIssueNames = Set(
    "vascular_disease",
    "mi",
    "myocardial_infarction",
    "cad",
    "coronary_artery_disease",
    "angina",
    "pci",
    "cabg",
    "peripheral_vascular_disease",
    "pvd"
  )

  def compute(facts: ClinicalFacts): Option[Result] =
    resolveBoolean(facts, DiagnosisKeys, DiagnosisIssueNames) match
      case Some(true) =>
        computeForConfirmedDiagnosis(facts)
      case Some(false) | None =>
        None

  private def computeForConfirmedDiagnosis(facts: ClinicalFacts): Option[Result] =
    (
      resolveAge(facts),
      resolveSex(facts),
      resolveBoolean(facts, HeartFailureKeys, HeartFailureIssueNames),
      resolveBoolean(facts, HypertensionKeys, HypertensionIssueNames),
      resolveBoolean(facts, DiabetesKeys, DiabetesIssueNames),
      resolveBoolean(facts, PriorStrokeKeys, PriorStrokeIssueNames),
      resolveBoolean(facts, VascularDiseaseKeys, VascularDiseaseIssueNames)
    ) match
      case (
            Some(age),
            Some(sexCategory),
            Some(heartFailure),
            Some(hypertension),
            Some(diabetes),
            Some(priorStroke),
            Some(vascularDisease)
          ) =>
        val riskFactors =
          Cha2ds2VascRiskFactor.fromAge(age).toList ++
            boolRiskFactor(heartFailure, Cha2ds2VascRiskFactor.CongestiveHeartFailure) ++
            boolRiskFactor(hypertension, Cha2ds2VascRiskFactor.Hypertension) ++
            boolRiskFactor(diabetes, Cha2ds2VascRiskFactor.DiabetesMellitus) ++
            boolRiskFactor(priorStroke, Cha2ds2VascRiskFactor.PriorStrokeTiaThromboembolism) ++
            boolRiskFactor(vascularDisease, Cha2ds2VascRiskFactor.VascularDisease) ++
            sexCategory.riskFactor.toList

        val total = Cha2ds2VascRiskFactor.totalPoints(riskFactors)
        val riskBand = Cha2ds2VascRiskBand.fromTotal(total, sexCategory)

        Some(Result(total = Some(total), riskBand = Some(riskBand), status = None))

      case _ =>
        Some(Result(total = None, riskBand = None, status = Some(Cha2ds2VascStatus.InsufficientData)))

  private[scoring] def resolveAge(facts: ClinicalFacts): Option[Int] =
    facts.firstValue(AgeKeys).flatMap(Parsing.parseInt).filter(_ >= 0)

  private[scoring] def resolveSex(facts: ClinicalFacts): Option[SexCategory] =
    facts.firstValue(SexKeys)
      .flatMap(Parsing.asText)
      .flatMap(SexCategory.parse)

  private[scoring] def resolveBoolean(
      facts: ClinicalFacts,
      keys: List[String],
      issueNames: Set[String]
  ): Option[Boolean] =
    facts.firstValue(keys).flatMap(Parsing.parseBoolean).orElse {
      if facts.hasIssue(issueNames) then Some(true) else None
    }

  private def boolRiskFactor(
      present: Boolean,
      riskFactor: Cha2ds2VascRiskFactor
  ): List[Cha2ds2VascRiskFactor] =
    if present then List(riskFactor) else Nil
