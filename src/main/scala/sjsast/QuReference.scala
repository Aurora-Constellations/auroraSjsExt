package docere.sjsast


case class QuReference (qu:String,name:String) extends SjsNode {
  override def merge(p: SjsNode): SjsNode = p

} 


object QuReference:
  def apply[T](quRef:GenAst.QuReference): QuReference = 
    val q = quRef.qu.mkString
    val n = quRef.ref.$refText
    QuReference(q,n)


      
