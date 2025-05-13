package docere.sjsast


case class QuReferences (refs: Set[docere.sjsast.QuReference]=Set.empty) extends SjsNode :
  override val name: String = "QuReferences"

  def merge(p: QuReferences): QuReferences = 
    val merged = refs |+| p.refs
    QuReferences(merged)

  override def merge(p:SjsNode): SjsNode = 
    merge(p.asInstanceOf[QuReferences])



object QuReferences:
  def apply[T](optQuRefs:Option[GenAst.QuReferences]): QuReferences = 
    val s:Set[QuReference] = optQuRefs.map{x => x.quRefs.toList}
      .getOrElse(Nil).map{(x:GenAst.QuReference) => QuReference(x)}.toSet
    QuReferences(s)


      
