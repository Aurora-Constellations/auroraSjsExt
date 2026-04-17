package org.aurora.sjsast.scoring

import org.aurora.sjsast.*

object ScoreWriteback:
  def apply(pcm: PCM, scoreValues: List[ClinicalValue], derivedIssues: List[IssueCoordinate]): PCM =
    val updatedClinical = updateClinicalSection(pcm.cio.get("Clinical").collect { case clinical: Clinical => clinical }, scoreValues)
    val updatedIssues = updateIssuesSection(pcm.cio.get("Issues").collect { case issues: Issues => issues }, derivedIssues)
    val updatedCio = LHMap.from(pcm.cio)

    updatedClinical match
      case Some(clinical) => updatedCio.update("Clinical", clinical)
      case None           => updatedCio.remove("Clinical")

    updatedIssues match
      case Some(issues) => updatedCio.update("Issues", issues)
      case None         => updatedCio.remove("Issues")

    PCM(cio = updatedCio)

  def intScore(name: String, value: Int): ClinicalValue =
    ClinicalValue(name = name, values = List(SingleValueUnit(IntValue(value), ScoringConstants.PlaceholderUnit)))

  def textScore(name: String, value: String): ClinicalValue =
    ClinicalValue(name = name, values = List(SingleValueUnit(StringValue(value), ScoringConstants.PlaceholderUnit)))

  def scoreValue(pcm: PCM, name: String): Option[Value] =
    pcm.cio
      .get("Clinical")
      .collect { case clinical: Clinical => clinical }
      .flatMap(_.ngc.find(_.name == ScoringConstants.ScoreGroupName))
      .flatMap(_.coordinates.collectFirst {
        case value: ClinicalValue if value.name == name && value.values.nonEmpty => value.values.head.value
      })

  private def updateClinicalSection(existing: Option[Clinical], scoreValues: List[ClinicalValue]): Option[Clinical] =
    existing match
      case Some(clinical) =>
        val baseGroups = clinical.ngc.filterNot(_.name == ScoringConstants.ScoreGroupName).toSeq
        if scoreValues.nonEmpty then
          Some(clinical.copy(ngc = LHSet.from(baseGroups :+ NGC(name = ScoringConstants.ScoreGroupName, coordinates = LHSet.from(scoreValues)))))
        else if clinical.ngc.exists(_.name == ScoringConstants.ScoreGroupName) then
          if baseGroups.nonEmpty then Some(clinical.copy(ngc = LHSet.from(baseGroups)))
          else None
        else
          Some(clinical)

      case None =>
        if scoreValues.nonEmpty then
          Some(Clinical(ngc = LHSet(NGC(name = ScoringConstants.ScoreGroupName, coordinates = LHSet.from(scoreValues)))))
        else
          None

  private def updateIssuesSection(existing: Option[Issues], derivedIssues: List[IssueCoordinate]): Option[Issues] =
    existing match
      case Some(issues) =>
        val baseIssues = issues.ic.filterNot(_.name.startsWith(ScoringConstants.DerivedIssuePrefix)).toSeq
        if derivedIssues.nonEmpty || issues.ic.exists(_.name.startsWith(ScoringConstants.DerivedIssuePrefix)) then
          Some(issues.copy(ic = LHSet.from(baseIssues ++ derivedIssues)))
        else
          Some(issues)

      case None =>
        if derivedIssues.nonEmpty then Some(Issues(ic = LHSet.from(derivedIssues)))
        else None
