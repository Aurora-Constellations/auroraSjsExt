package org.aurora.sjsast.scoring

import org.aurora.sjsast.{Clinical, ClinicalValue, Issues, PCM, SingleValueUnit}

import scala.collection.mutable

final case class ClinicalFacts(
    values: Map[String, List[SingleValueUnit]],
    issueNames: Set[String]
):
  def firstValue(keys: Iterable[String]): Option[SingleValueUnit] =
    keys.iterator
      .map(values.getOrElse(_, Nil))
      .find(_.nonEmpty)
      .flatMap(_.headOption)

  def hasIssue(names: Set[String]): Boolean =
    issueNames.exists(names.contains)

object ClinicalFacts:
  def from(pcm: PCM): ClinicalFacts =
    val values = mutable.LinkedHashMap.empty[String, List[SingleValueUnit]]

    pcm.cio.get("Clinical").collect { case clinical: Clinical => clinical }.foreach { clinical =>
      clinical.ngc
        .filterNot(_.name == ScoringConstants.ScoreGroupName)
        .foreach { group =>
          group.coordinates.foreach {
            case value: ClinicalValue =>
              val key = Parsing.normalizeName(value.name)
              if key.nonEmpty then
                val existing = values.getOrElse(key, Nil)
                values.update(key, existing ++ value.values)
            case _ => ()
          }
        }
    }

    val issueNames = pcm.cio
      .get("Issues")
      .collect { case issues: Issues => issues }
      .map(_.ic.map(_.name).map(Parsing.normalizeName).filterNot(_.startsWith(ScoringConstants.DerivedIssuePrefix)).toSet)
      .getOrElse(Set.empty)

    ClinicalFacts(values = values.toMap, issueNames = issueNames)
