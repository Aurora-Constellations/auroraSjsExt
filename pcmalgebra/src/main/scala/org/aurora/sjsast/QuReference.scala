package org.aurora.sjsast


case class QuReference (qu:String,name:String) extends SjsNode { 
  // def merge(qref:QuReference) : QuReference = 
  //   val q = qu |+| qref.qu
  //   QuReference(q,name)
  override def merge(p: SjsNode): SjsNode =
    val other = p.asInstanceOf[QuReference]
    QuReference(qu + other.qu, name)

} 


object QuReference:
  def apply[T](quRef:GenAst.QuReference): QuReference = 
    val q = quRef.qu.toList.map(_.query).mkString("")  // Fix: actually extract the qualifier
    val n = quRef.ref.$refText
    QuReference(q,n)


      
