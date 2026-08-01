package org.aurora.sjsast.scoring

import org.aurora.sjsast.{Clinical, ClinicalItem, Issues, PCM, SingleValueUnit}

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
  val Empty: ClinicalFacts =
    ClinicalFacts(values = Map.empty, issueNames = Set.empty)

  def from(pcm: PCM): ClinicalFacts =
    val values = mutable.LinkedHashMap.empty[String, List[SingleValueUnit]]

    pcm.cio.get("Clinical").collect { case clinical: Clinical => clinical }.foreach { clinical =>
      clinical.ngc
        .filterNot(_.name == ScoringConstants.ScoreGroupName)
        .foreach { group =>
          group.coordinates.foreach {
            // Updated type match to ClinicalItem
            case value: ClinicalItem =>
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
      .map { issues =>
        issues.ic
          .map(_.name)
          .map(Parsing.normalizeName)
          .filterNot(_.startsWith(ScoringConstants.DerivedIssuePrefix))
          .toSet
      }
      .getOrElse(Set.empty)

    ClinicalFacts(values = values.toMap, issueNames = issueNames)