package docere.sjsast

import scala.scalajs.js

case class ClinicalCoordinateValue(
  name: String,
  narrative: Set[NL_STATEMENT] = Set.empty,
  refs: Set[RefCoordinate] = Set.empty,
  qu: Set[QU] = Set.empty
) extends SjsNode:

  def merge(cc: ClinicalCoordinateValue): ClinicalCoordinateValue =
    val narratives = narrative |+| cc.narrative
    val result = refs |+| cc.refs
    val qumerge = qu |+| cc.qu
    ClinicalCoordinateValue(name, narratives, result,qumerge)

  override def merge(p: SjsNode): SjsNode =
    merge(p.asInstanceOf[ClinicalCoordinateValue])

object ClinicalCoordinateValue:
  def apply(c: GenAst.ClinicalCoordinateValue): ClinicalCoordinateValue =
    val dyn = c.asInstanceOf[js.Dynamic]

    val name = dyn.selectDynamic("name").asInstanceOf[String]

    val narratives = dyn.selectDynamic("narrative")
      .asInstanceOf[js.Array[js.Dynamic]]
      .map(n => NL_STATEMENT(n.selectDynamic("name").asInstanceOf[String]))
      .toSet

    val refs = dyn.selectDynamic("refs")
      .asInstanceOf[js.Array[js.Dynamic]]
      .map(r => RefCoordinate(r.selectDynamic("$refText").asInstanceOf[String]))
      .toSet

    val qus = dyn.selectDynamic("qu").asInstanceOf[js.Array[js.Dynamic]].map{p =>  QU(p.selectDynamic("qu").asInstanceOf[String])}.toSet

    ClinicalCoordinateValue(name, narratives, refs, qus)
