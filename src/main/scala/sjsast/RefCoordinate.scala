package docere.sjsast


case class RefCoordinate (name:String) extends SjsNode {
  override def merge(p: SjsNode): SjsNode = p

} 


object RefCoordinate:
  def apply[T](langref:GenAst.LangiumReference[T]): RefCoordinate = 
      RefCoordinate(langref.$refText)


      
