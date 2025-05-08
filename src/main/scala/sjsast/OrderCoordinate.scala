package docere.sjsast

case class OrderCoordinate (name:String, refs:Set[RefCoordinate]=Set.empty, narrative:Set[NL_STATEMENT]=Set.empty, qu: Set[QU] = Set.empty) extends SjsNode:
  def merge (oc:OrderCoordinate):OrderCoordinate = 
    val result = refs |+| oc.refs
    val narratives = narrative |+| oc.narrative
    val qumerge = qu |+| oc.qu
    OrderCoordinate(name,result, narratives, qumerge)

  override def merge(p: SjsNode): SjsNode =
    merge(p.asInstanceOf[OrderCoordinate])

object OrderCoordinate :
  def apply(o: GenAst.OrderCoordinate): OrderCoordinate = 
    val x = o.refs.toList.map{r =>  RefCoordinate(r.$refText)}.toSet
    val narratives = o.narrative.toList.map{n =>  NL_STATEMENT(n.name)}.toSet
    val qus = o.qu.toList.map{p =>  QU(p.query)}.toSet
    OrderCoordinate(o.name,x, narratives, qus)

