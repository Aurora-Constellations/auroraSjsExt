package org.aurora.sjsast


case class QuReferences (refs: Set[QuReference]=Set.empty) extends SjsNode :
  override val name: String = "QuReferences"

  def merge(p: QuReferences): QuReferences = 
    val merged = refs |+| p.refs
    QuReferences(merged)

  override def merge(p:SjsNode): SjsNode = 
    merge(p.asInstanceOf[QuReferences])



object QuReferences:
  def apply[T](optQuRefs:Option[GenAst.QuReferences]): QuReferences = 
    val s:Set[QuReference] = optQuRefs
      .map{genQuRefs => genQuRefs.quRefs.toList}
      .getOrElse(Nil)
      .map{ref => QuReference(ref)}.toSet

    // println(s"QuReferences applied with size:${s.size} refs:${s}")  
    QuReferences(s)






      
