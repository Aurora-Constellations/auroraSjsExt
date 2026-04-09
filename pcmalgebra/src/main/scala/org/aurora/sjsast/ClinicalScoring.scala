package org.aurora.sjsast

import scala.collection.mutable

object ClinicalScoring:

  final case class ScoreUpdate(
      pcm: PCM,
      derivedModuleImports: Map[String, String] = Map.empty
  )

  private val ScoreGroupName     = "Scores:"
  private val DerivedIssuePrefix = "score_"
  private val PlaceholderUnit    = "_"

  private val AgeKeys = List("age", "age_years", "ageyears")
  private val SexKeys = List("sex", "gender", "sex_for_score")

  private val AfDiagnosisKeys = List("atrial_fibrillation", "atrial_flutter", "af")
  private val HeartFailureKeys = List("heart_failure", "cha2ds2_vasc_heart_failure")
  private val HypertensionKeys = List("hypertension", "cha2ds2_vasc_hypertension")
  private val DiabetesKeys = List("diabetes", "cha2ds2_vasc_diabetes")
  private val PriorStrokeKeys = List("prior_stroke_tia_te", "prior_stroke_tia_thromboembolism", "prior_stroke", "prior_tia")
  private val VascularDiseaseKeys = List("vascular_disease", "cha2ds2_vasc_vascular_disease")

  private val GcsEyeKeys = List("gcs_eye", "glasgow_eye", "gcs_eye_response", "glasgow_coma_scale_eye")
  private val GcsVerbalKeys = List("gcs_verbal", "glasgow_verbal", "gcs_verbal_response", "glasgow_coma_scale_verbal")
  private val GcsMotorKeys = List("gcs_motor", "glasgow_motor", "gcs_motor_response", "glasgow_coma_scale_motor")
  private val GcsTotalKeys = List("gcs_total", "glasgow_total", "glasgow_coma_scale_total")

  private val AfDiagnosisIssueNames = Set("atrial_fibrillation", "atrial_flutter", "af", "afib", "a_fib", "a_flutter")
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

  def apply(pcm: PCM): ScoreUpdate =
    val facts = ClinicalFacts.from(pcm)
    val scoreValues = mutable.ListBuffer.empty[ClinicalValue]
    val derivedIssues = mutable.ListBuffer.empty[IssueCoordinate]
    val derivedModuleImports = mutable.LinkedHashMap.empty[String, String]

    computeGcs(facts).foreach { result =>
      result.total.foreach(total => scoreValues += intScore("gcs_total", total))
      result.source.foreach(source => scoreValues += textScore("gcs_total_source", source))
      result.severity.foreach(severity => scoreValues += textScore("gcs_severity", severity))
      result.status.foreach(status => scoreValues += textScore("gcs_status", status))
      result.derivedIssue.foreach { issue =>
        derivedIssues += issue
        issue.fromMods.headOption.foreach(module => derivedModuleImports += (module -> issue.name))
      }
    }

    computeCha2Ds2Vasc(facts).foreach { result =>
      result.total.foreach(total => scoreValues += intScore("cha2ds2_vasc_total", total))
      result.riskBand.foreach(band => scoreValues += textScore("cha2ds2_vasc_risk_band", band))
      result.status.foreach(status => scoreValues += textScore("cha2ds2_vasc_status", status))
      result.derivedIssue.foreach { issue =>
        derivedIssues += issue
        issue.fromMods.headOption.foreach(module => derivedModuleImports += (module -> issue.name))
      }
    }

    val updatedClinical = updateClinicalSection(pcm.cio.get("Clinical").collect { case clinical: Clinical => clinical }, scoreValues.toList)
    val updatedIssues = updateIssuesSection(pcm.cio.get("Issues").collect { case issues: Issues => issues }, derivedIssues.toList)
    val updatedCio = LHMap.from(pcm.cio)

    updatedClinical match
      case Some(clinical) => updatedCio.update("Clinical", clinical)
      case None           => updatedCio.remove("Clinical")

    updatedIssues match
      case Some(issues) => updatedCio.update("Issues", issues)
      case None         => updatedCio.remove("Issues")

    ScoreUpdate(
      pcm = PCM(cio = updatedCio),
      derivedModuleImports = derivedModuleImports.toMap
    )

  private final case class ClinicalFacts(
      values: Map[String, List[SingleValueUnit]],
      issueNames: Set[String]
  ):
    def firstValue(keys: Iterable[String]): Option[SingleValueUnit] =
      keys.iterator
        .map(values.getOrElse(_, Nil))
        .find(_.nonEmpty)
        .flatMap(_.headOption)

    def hasIssue(issueNames: Set[String]): Boolean =
      this.issueNames.exists(issueNames.contains)

  private object ClinicalFacts:
    def from(pcm: PCM): ClinicalFacts =
      val values = mutable.LinkedHashMap.empty[String, List[SingleValueUnit]]

      pcm.cio.get("Clinical").collect { case clinical: Clinical => clinical }.foreach { clinical =>
        clinical.ngc
          .filterNot(_.name == ScoreGroupName)
          .foreach { group =>
            group.coordinates.foreach {
              case value: ClinicalValue =>
                val key = normalizeName(value.name)
                if key.nonEmpty then
                  val existing = values.getOrElse(key, Nil)
                  values.update(key, existing ++ value.values)
              case _ => ()
            }
          }
      }

      val issues = pcm.cio
        .get("Issues")
        .collect { case issueSection: Issues => issueSection }
        .map(_.ic.map(_.name).map(normalizeName).filterNot(_.startsWith(DerivedIssuePrefix)).toSet)
        .getOrElse(Set.empty)

      ClinicalFacts(values = values.toMap, issueNames = issues)

  private enum SexCategory:
    case Female
    case NotFemale

  private final case class GcsComponent(score: Option[Int], notTestable: Boolean = false)
  private final case class GcsResult(
      total: Option[Int],
      severity: Option[String],
      source: Option[String],
      status: Option[String],
      derivedIssue: Option[IssueCoordinate]
  )

  private final case class AfRiskResult(
      total: Option[Int],
      riskBand: Option[String],
      status: Option[String],
      derivedIssue: Option[IssueCoordinate]
  )

  private def computeGcs(facts: ClinicalFacts): Option[GcsResult] =
    val eye = resolveGcsComponent(facts, GcsEyeKeys, max = 4, synonyms = Map(
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
    val verbal = resolveGcsComponent(facts, GcsVerbalKeys, max = 5, synonyms = Map(
      "orientated" -> 5,
      "oriented" -> 5,
      "confused" -> 4,
      "words" -> 3,
      "inappropriate_words" -> 3,
      "sounds" -> 2,
      "incomprehensible_sounds" -> 2,
      "none" -> 1
    ))
    val motor = resolveGcsComponent(facts, GcsMotorKeys, max = 6, synonyms = Map(
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
    val manualTotal = resolveInt(facts, GcsTotalKeys)

    val hasAnyGcsData =
      eye.score.nonEmpty || verbal.score.nonEmpty || motor.score.nonEmpty || manualTotal.nonEmpty ||
        eye.notTestable || verbal.notTestable || motor.notTestable

    if !hasAnyGcsData then
      None
    else
      val hasNotTestable = List(eye, verbal, motor).exists(_.notTestable)
      val componentScores = List(eye.score, verbal.score, motor.score)
      val componentTotal =
        if componentScores.forall(_.nonEmpty) then Some(componentScores.flatten.sum)
        else None

      val total =
        if hasNotTestable then None
        else componentTotal.orElse(manualTotal)
      val source =
        if hasNotTestable then None
        else total.map(_ => if componentTotal.nonEmpty then "derived" else "manual")
      val status =
        if hasNotTestable then Some("not_testable")
        else if total.isEmpty then Some("incomplete")
        else None
      val severity = total.map(gcsSeverity)
      val derivedIssue = severity.flatMap {
        case "severe"   => Some(derivedIssueFor("score_gcs_severe", "gcs_severe"))
        case "moderate" => Some(derivedIssueFor("score_gcs_moderate", "gcs_moderate"))
        case _          => None
      }

      Some(GcsResult(total, severity, source, status, derivedIssue))

  private def computeCha2Ds2Vasc(facts: ClinicalFacts): Option[AfRiskResult] =
    resolveBoolean(facts, AfDiagnosisKeys, AfDiagnosisIssueNames) match
      case Some(true) =>
        (
          resolveInt(facts, AgeKeys),
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
            val ageScore =
              if age >= 75 then 2
              else if age >= 65 then 1
              else 0

            val total =
              ageScore +
                boolPoints(heartFailure) +
                boolPoints(hypertension) +
                boolPoints(diabetes) +
                doubleBoolPoints(priorStroke) +
                boolPoints(vascularDisease) +
                (if sex == SexCategory.Female then 1 else 0)

            val riskBand = cha2ds2VascBand(total, sex)
            val derivedIssue = riskBand match
              case "high" =>
                Some(derivedIssueFor("score_af_stroke_risk_high", "af_stroke_risk_high"))
              case "intermediate" =>
                Some(derivedIssueFor("score_af_stroke_risk_intermediate", "af_stroke_risk_intermediate"))
              case _ =>
                None

            Some(AfRiskResult(Some(total), Some(riskBand), None, derivedIssue))

          case _ =>
            Some(AfRiskResult(None, None, Some("insufficient_data"), None))

      case Some(false) =>
        None

      case None =>
        None

  private def resolveBoolean(facts: ClinicalFacts, keys: List[String], issueNames: Set[String]): Option[Boolean] =
    facts.firstValue(keys).flatMap(parseBoolean).orElse {
      if facts.hasIssue(issueNames) then Some(true)
      else None
    }

  private def resolveInt(facts: ClinicalFacts, keys: List[String]): Option[Int] =
    facts.firstValue(keys).flatMap(parseInt)

  private def resolveSex(facts: ClinicalFacts): Option[SexCategory] =
    facts.firstValue(SexKeys).flatMap { value =>
      asText(value) match
        case Some("female") | Some("f") | Some("woman") => Some(SexCategory.Female)
        case Some("male") | Some("m") | Some("man") | Some("not_female") => Some(SexCategory.NotFemale)
        case _ => None
    }

  private def resolveGcsComponent(
      facts: ClinicalFacts,
      keys: List[String],
      max: Int,
      synonyms: Map[String, Int]
  ): GcsComponent =
    facts.firstValue(keys) match
      case None => GcsComponent(None)
      case Some(value) =>
        asText(value) match
          case Some(text) if Set("nt", "not_testable").contains(text) =>
            GcsComponent(None, notTestable = true)
          case Some(text) if synonyms.contains(text) =>
            GcsComponent(Some(synonyms(text)))
          case _ =>
            parseInt(value).filter(score => score >= 1 && score <= max) match
              case Some(score) => GcsComponent(Some(score))
              case None        => GcsComponent(None)

  private def updateClinicalSection(existing: Option[Clinical], scoreValues: List[ClinicalValue]): Option[Clinical] =
    existing match
      case Some(clinical) =>
        val baseGroups = clinical.ngc.filterNot(_.name == ScoreGroupName).toSeq
        if scoreValues.nonEmpty then
          Some(clinical.copy(ngc = LHSet.from(baseGroups :+ NGC(name = ScoreGroupName, coordinates = LHSet.from(scoreValues)) )))
        else if clinical.ngc.exists(_.name == ScoreGroupName) then
          if baseGroups.nonEmpty then Some(clinical.copy(ngc = LHSet.from(baseGroups)))
          else None
        else
          Some(clinical)

      case None =>
        if scoreValues.nonEmpty then
          Some(Clinical(ngc = LHSet(NGC(name = ScoreGroupName, coordinates = LHSet.from(scoreValues)))))
        else
          None

  private def updateIssuesSection(existing: Option[Issues], derivedIssues: List[IssueCoordinate]): Option[Issues] =
    existing match
      case Some(issues) =>
        val baseIssues = issues.ic.filterNot(_.name.startsWith(DerivedIssuePrefix)).toSeq
        if derivedIssues.nonEmpty || issues.ic.exists(_.name.startsWith(DerivedIssuePrefix)) then
          Some(issues.copy(ic = LHSet.from(baseIssues ++ derivedIssues)))
        else
          Some(issues)

      case None =>
        if derivedIssues.nonEmpty then Some(Issues(ic = LHSet.from(derivedIssues)))
        else None

  private def boolPoints(value: Boolean): Int =
    if value then 1 else 0

  private def doubleBoolPoints(value: Boolean): Int =
    if value then 2 else 0

  private def gcsSeverity(total: Int): String =
    if total <= 8 then "severe"
    else if total <= 12 then "moderate"
    else "mild"

  private def cha2ds2VascBand(total: Int, sex: SexCategory): String =
    sex match
      case SexCategory.Female =>
        if total >= 3 then "high"
        else if total == 2 then "intermediate"
        else "low"
      case SexCategory.NotFemale =>
        if total >= 2 then "high"
        else if total == 1 then "intermediate"
        else "low"

  private def derivedIssueFor(aliasName: String, moduleName: String): IssueCoordinate =
    IssueCoordinate(name = aliasName, fromMods = List(moduleName))

  private def intScore(name: String, value: Int): ClinicalValue =
    ClinicalValue(name = name, values = List(SingleValueUnit(IntValue(value), PlaceholderUnit)))

  private def textScore(name: String, value: String): ClinicalValue =
    ClinicalValue(name = name, values = List(SingleValueUnit(StringValue(value), PlaceholderUnit)))

  private def parseBoolean(value: SingleValueUnit): Option[Boolean] =
    value.value match
      case BoolValue(v) => Some(v)
      case IntValue(v)  => Some(v != 0)
      case DoubleValue(v) => Some(v != 0)
      case StringValue(v) =>
        normalizeName(v) match
          case "true" | "yes" | "y" | "present" | "positive" | "1"  => Some(true)
          case "false" | "no" | "n" | "absent" | "negative" | "0"   => Some(false)
          case "unknown" | "incomplete" | "not_assessed" | "not_assessable" => None
          case _ => None

  private def parseInt(value: SingleValueUnit): Option[Int] =
    value.value match
      case IntValue(v)    => Some(v)
      case DoubleValue(v) => Some(v.toInt)
      case BoolValue(v)   => Some(if v then 1 else 0)
      case StringValue(v) =>
        normalizeName(v) match
          case "unknown" | "nt" | "not_testable" | "incomplete" => None
          case other => other.toIntOption

  private def asText(value: SingleValueUnit): Option[String] =
    value.value match
      case StringValue(v) => Some(normalizeName(v))
      case IntValue(v)    => Some(v.toString)
      case DoubleValue(v) => Some(v.toInt.toString)
      case BoolValue(v)   => Some(if v then "true" else "false")

  private def normalizeName(value: String): String =
    value
      .trim
      .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
      .toLowerCase
      .replaceAll("[^a-z0-9]+", "_")
      .stripPrefix("_")
      .stripSuffix("_")
