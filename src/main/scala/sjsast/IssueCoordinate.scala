package docere.sjsast

case class IssueCoordinate(name: String, refs: Set[RefCoordinate] = Set.empty, narrative:Set[NL_STATEMENT]=Set.empty, qu: Set[QU] = Set.empty) extends SjsNode:
  def merge(ic: IssueCoordinate): IssueCoordinate =
    val result = refs |+| ic.refs
    val narratives = narrative |+| ic.narrative
    val qumerge = qu |+| ic.qu
    IssueCoordinate(name, result, narratives, qumerge)

  override def merge(p: SjsNode): SjsNode =
    merge(p.asInstanceOf[IssueCoordinate])

object IssueCoordinate:
  def apply(i: GenAst.IssueCoordinate): IssueCoordinate =
    val x = i.refs.toList.map { r => RefCoordinate(r.$refText) }.toSet
    val narratives = i.narrative.toList.map{n =>  NL_STATEMENT(n.name)}.toSet
    val qus = i.qu.toList.map{p =>  QU(p.query)}.toSet
    IssueCoordinate(i.name, x, narratives, qus)
