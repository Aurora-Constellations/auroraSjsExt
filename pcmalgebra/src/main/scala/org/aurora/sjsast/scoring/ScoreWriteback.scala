package org.aurora.sjsast.scoring

import org.aurora.sjsast.*

/** Replaces the fields owned by clinical scoring while preserving unrelated
  * content in the Clinical and Issues sections.
  *
  * A scoring pass is a refresh, not an additive join: an owned field omitted
  * from `scoreValues` and an owned issue omitted from `derivedIssues` are
  * removed. This prevents old totals, statuses, and guidance issues from
  * surviving after the source facts change or disappear.
  */
object ScoreWriteback:
  private val OwnedScoreNames = Set(
    "gcs_total",
    "gcs_total_source",
    "gcs_severity",
    "gcs_status",
    "cha2ds2_vasc_total",
    "cha2ds2_vasc_risk_band",
    "cha2ds2_vasc_status"
  )

  private val OwnedIssueNames = Set(
    "score_gcs_severe",
    "score_gcs_moderate",
    "score_af_stroke_risk_high",
    "score_af_stroke_risk_intermediate"
  )

  def apply(
      pcm: PCM,
      scoreValues: List[ClinicalItem],
      derivedIssues: List[IssueCoordinate]
  ): PCM =
    val existingClinical = pcm.cio.get("Clinical").collect { case clinical: Clinical => clinical }
    val existingIssues = pcm.cio.get("Issues").collect { case issues: Issues => issues }
    val updatedClinical = updateClinicalSection(existingClinical, normalizeScores(scoreValues))
    val updatedIssues = updateIssuesSection(existingIssues, normalizeIssues(derivedIssues))
    val updatedCio = LHMap.from(pcm.cio)

    updatedClinical match
      case Some(clinical) => updatedCio.update("Clinical", clinical)
      case None           => updatedCio.remove("Clinical")

    updatedIssues match
      case Some(issues) => updatedCio.update("Issues", issues)
      case None         => updatedCio.remove("Issues")

    pcm.copy(cio = updatedCio)

  def intScore(name: String, value: Int): ClinicalItem =
    ClinicalItem(
      name = name,
      values = List(SingleValueUnit(IntValue(value), ScoringConstants.PlaceholderUnit))
    )

  def textScore(name: String, value: String): ClinicalItem =
    ClinicalItem(
      name = name,
      values = List(SingleValueUnit(StringValue(value), ScoringConstants.PlaceholderUnit))
    )

  def scoreValue(pcm: PCM, name: String): Option[Value] =
    pcm.cio
      .get("Clinical")
      .collect { case clinical: Clinical => clinical }
      .toSeq
      .flatMap(_.ngc.iterator.filter(_.name == ScoringConstants.ScoreGroupName))
      .flatMap(_.coordinates)
      .find(item => item.name == name && item.values.nonEmpty)
      .flatMap(_.values.headOption)
      .map(_.value)

  private def normalizeScores(scoreValues: List[ClinicalItem]): List[ClinicalItem] =
    val byName = scala.collection.mutable.LinkedHashMap.empty[String, ClinicalItem]
    scoreValues.foreach { score =>
      require(
        OwnedScoreNames.contains(score.name),
        s"ScoreWriteback cannot manage unowned score field '${score.name}'"
      )
      byName.update(score.name, score)
    }
    byName.values.toList

  private def normalizeIssues(derivedIssues: List[IssueCoordinate]): List[IssueCoordinate] =
    val byName = scala.collection.mutable.LinkedHashMap.empty[String, IssueCoordinate]
    derivedIssues.foreach { issue =>
      require(
        OwnedIssueNames.contains(issue.name),
        s"ScoreWriteback cannot manage unowned derived issue '${issue.name}'"
      )
      byName.update(issue.name, issue)
    }
    byName.values.toList

  private def updateClinicalSection(
      existing: Option[Clinical],
      scoreValues: List[ClinicalItem]
  ): Option[Clinical] =
    existing match
      case Some(clinical) =>
        val scoreGroups = clinical.ngc.iterator
          .filter(_.name == ScoringConstants.ScoreGroupName)
          .toList
        val baseGroups = clinical.ngc.iterator
          .filterNot(_.name == ScoringConstants.ScoreGroupName)
          .toList
        val preservedScoreValues = scoreGroups.iterator
          .flatMap(_.coordinates)
          .filterNot(item => OwnedScoreNames.contains(item.name))
          .toList
        val refreshedValues = LHSet.from(preservedScoreValues ++ scoreValues)
        val scoreNarratives = LHSet.from(scoreGroups.iterator.flatMap(_.narratives))
        val scoreRefs = LHSet.from(scoreGroups.iterator.flatMap(_.refs))
        val keepScoreGroup =
          refreshedValues.nonEmpty || scoreNarratives.nonEmpty || scoreRefs.nonEmpty
        val refreshedGroups =
          if keepScoreGroup then
            baseGroups :+ NGC(
              name = ScoringConstants.ScoreGroupName,
              narratives = scoreNarratives,
              coordinates = refreshedValues,
              refs = scoreRefs
            )
          else baseGroups

        Some(clinical.copy(ngc = LHSet.from(refreshedGroups)))

      case None =>
        if scoreValues.nonEmpty then
          Some(
            Clinical(
              ngc = LHSet(
                NGC(
                  name = ScoringConstants.ScoreGroupName,
                  coordinates = LHSet.from(scoreValues)
                )
              )
            )
          )
        else None

  private def updateIssuesSection(
      existing: Option[Issues],
      derivedIssues: List[IssueCoordinate]
  ): Option[Issues] =
    existing match
      case Some(issues) =>
        val baseIssues = issues.ic.iterator
          .filterNot(issue => OwnedIssueNames.contains(issue.name))
          .toList
        Some(issues.copy(ic = LHSet.from(baseIssues ++ derivedIssues)))

      case None =>
        if derivedIssues.nonEmpty then Some(Issues(ic = LHSet.from(derivedIssues)))
        else None
