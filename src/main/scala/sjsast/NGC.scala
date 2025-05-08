package docere.sjsast

import scala.scalajs.js

case class NGC(
  name: String,
  ccoords: Set[ClinicalCoordinateValue],
  narrative: Set[NL_STATEMENT] = Set.empty,
  refs: Set[RefCoordinate] = Set.empty
) extends SjsNode:

  def merge(n: NGC): NGC =
    val mergedCoords = (ccoords ++ n.ccoords)
      .groupBy(_.name)
      .map { case (_, dups) => dups.reduce(_.merge(_)) }
      .toSet

    val mergedNarratives = narrative |+| n.narrative
    val mergedRefs = refs |+| n.refs

    NGC(name, mergedCoords, mergedNarratives, mergedRefs)

  override def merge(p: SjsNode): SjsNode = merge(p.asInstanceOf[NGC])

object NGC:
  def apply(n: GenAst.NGC): NGC =
    val narratives = n.narrative.toList.map(n => NL_STATEMENT(n.name)).toSet
    val cc = n.coord.toList.map(c => ClinicalCoordinateValue(c)).toSet
    val refs = n.refs.toList.map(r => RefCoordinate(r.$refText)).toSet
    NGC(n.name, cc, narratives, refs)