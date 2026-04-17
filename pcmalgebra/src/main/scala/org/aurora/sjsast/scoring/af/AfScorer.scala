package org.aurora.sjsast.scoring.af

import org.aurora.sjsast.IssueCoordinate
import org.aurora.sjsast.scoring.{ClinicalFacts, Parsing}

object AfScorer:
  final case class Result(
      total: Option[Int],
      riskBand: Option[String],
      status: Option[String],
      derivedIssue: Option[IssueCoordinate]
  )

  private enum SexCategory:
    case Female
    case NotFemale

  private val AgeKeys = List("age", "age_years", "ageyears")
  private val SexKeys = List("sex", "gender", "sex_for_score")
  private val DiagnosisKeys = List("atrial_fibrillation", "atrial_flutter", "af")
  private val HeartFailureKeys = List("heart_failure", "cha2ds2_vasc_heart_failure")
  private val HypertensionKeys = List("hypertension", "cha2ds2_vasc_hypertension")
  private val DiabetesKeys = List("diabetes", "cha2ds2_vasc_diabetes")
  private val PriorStrokeKeys = List("prior_stroke_tia_te", "prior_stroke_tia_thromboembolism", "prior_stroke", "prior_tia")
  private val VascularDiseaseKeys = List("vascular_disease", "cha2ds2_vasc_vascular_disease")

  private val DiagnosisIssueNames = Set("atrial_fibrillation", "atrial_flutter", "af", "afib", "a_fib", "a_flutter")
  private val HeartFailureIssueNames = Set("heart_failure", "congestive_heart_failure", "chf", "hf")
  private val HypertensionIssueNames = Set("hypertension", "htn")
  private val DiabetesIssueNames = Set("diabetes", "diabetes_mellitus", "dm")
  private val PriorStrokeIssueNames = Set("stroke", "tia", "thromboembolism", "prior_stroke", "prior_tia")
  private val VascularDiseaseIssueNames = Set(
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
        (
          facts.firstValue(AgeKeys).flatMap(Parsing.parseInt),
          resolveSex(facts),
          resolveBoolean(facts, HeartFailureKeys, HeartFailureIssueNames),
          resolveBoolean(facts, HypertensionKeys, HypertensionIssueNames),
          resolveBoolean(facts, DiabetesKeys, DiabetesIssueNames),
          resolveBoolean(facts, PriorStrokeKeys, PriorStrokeIssueNames),
          resolveBoolean(facts, VascularDiseaseKeys, VascularDiseaseIssueNames)
        ) match
          case (
                Some(age),
                Some(sex),
                Some(heartFailure),
                Some(hypertension),
                Some(diabetes),
                Some(priorStroke),
                Some(vascularDisease)
              ) =>
            val total =
              ageScore(age) +
                boolPoints(heartFailure) +
                boolPoints(hypertension) +
                boolPoints(diabetes) +
                doubleBoolPoints(priorStroke) +
                boolPoints(vascularDisease) +
                (if sex == SexCategory.Female then 1 else 0)

            val riskBand = riskBandFor(total, sex)
            val derivedIssue = riskBand match
              case "high" =>
                Some(IssueCoordinate(name = "score_af_stroke_risk_high", fromMods = List("af_stroke_risk_high")))
              case "intermediate" =>
                Some(IssueCoordinate(name = "score_af_stroke_risk_intermediate", fromMods = List("af_stroke_risk_intermediate")))
              case _ =>
                None

            Some(Result(Some(total), Some(riskBand), None, derivedIssue))

          case _ =>
            Some(Result(None, None, Some("insufficient_data"), None))

      case Some(false) => None
      case None        => None

  private def resolveBoolean(facts: ClinicalFacts, keys: List[String], issueNames: Set[String]): Option[Boolean] =
    facts.firstValue(keys).flatMap(Parsing.parseBoolean).orElse {
      if facts.hasIssue(issueNames) then Some(true) else None
    }

  private def resolveSex(facts: ClinicalFacts): Option[SexCategory] =
    facts.firstValue(SexKeys).flatMap { value =>
      Parsing.asText(value) match
        case Some("female") | Some("f") | Some("woman") => Some(SexCategory.Female)
        case Some("male") | Some("m") | Some("man") | Some("not_female") => Some(SexCategory.NotFemale)
        case _ => None
    }

  private def ageScore(age: Int): Int =
    if age >= 75 then 2
    else if age >= 65 then 1
    else 0

  private def riskBandFor(total: Int, sex: SexCategory): String =
    sex match
      case SexCategory.Female =>
        if total >= 3 then "high"
        else if total == 2 then "intermediate"
        else "low"
      case SexCategory.NotFemale =>
        if total >= 2 then "high"
        else if total == 1 then "intermediate"
        else "low"

  private def boolPoints(value: Boolean): Int =
    if value then 1 else 0

  private def doubleBoolPoints(value: Boolean): Int =
    if value then 2 else 0
