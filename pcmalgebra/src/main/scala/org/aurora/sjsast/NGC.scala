package org.aurora.sjsast

import scala.scalajs.js

case class NGC(
  name: String,
  ccoords: Set[ClinicalCoordinateValue],
  narrative: Set[NL_STATEMENT] = Set.empty,
  quRefs: QuReferences 
) extends SjsNode:

  def merge(n: NGC): NGC =
    val mergedCoords = (ccoords ++ n.ccoords)
      .groupBy(_.name)
      .map { case (_, dups) => dups.reduce(_.merge(_)) }
      .toSet

    val mergedNarratives = narrative |+| n.narrative
    val mergedRefs = quRefs.merge(n.quRefs)
    NGC(name, mergedCoords, mergedNarratives, mergedRefs)

  override def merge(p: SjsNode): SjsNode = merge(p.asInstanceOf[NGC])

object NGC:
  def apply(n: GenAst.NGC): NGC =

    val quRefs = QuReferences(n.qurc.toOption)
    val narratives = n.narrative.toList.map(n => NL_STATEMENT(n.name)).toSet
    val cc = n.coord.toList.map(c => ClinicalCoordinateValue(c)).toSet
    NGC(n.name, cc, narratives, quRefs)