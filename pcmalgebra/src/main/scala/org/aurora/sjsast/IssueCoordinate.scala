package org.aurora.sjsast


case class IssueCoordinate (name:String, narratives:Set[String]) extends SjsNode :

  def merge(i: IssueCoordinate): IssueCoordinate =  
    val narratives = this.narratives |+| i.narratives
    IssueCoordinate(i.name,narratives)

  override def merge(p: SjsNode): SjsNode = merge(p.asInstanceOf[IssueCoordinate])


object IssueCoordinate : 
  def apply(i: GenAst.IssueCoordinate): IssueCoordinate = 
    val narratives = i.narrative.toList.map{n => n.name}.toSet
    IssueCoordinate(i.name, narratives)
