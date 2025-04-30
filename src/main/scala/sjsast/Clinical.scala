package docere.sjsast

case class Clinical(ngc:Set[NGC], narrative:Set[NL_STATEMENT]=Set.empty) extends SjsNode :
  override val name = "Clinical" 

  def merge(c: Clinical): Clinical =
    val narratives = narrative |+| c.narrative
    Clinical(ngc merge c.ngc, narratives)
  override def merge(p: SjsNode): SjsNode = 
    merge(p.asInstanceOf[Clinical])

object Clinical :
  def apply(c: GenAst.Clinical): Clinical = 
    val g = c.namedGroups.toList.map{NGC(_)}.toSet
    val narratives = c.narrative.toList.map{n =>  NL_STATEMENT(n.name)}.toSet
    Clinical(g, narratives)