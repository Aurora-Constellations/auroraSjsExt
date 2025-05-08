package docere.sjsast

type CIO = Clinical|Issues|Orders

case class PCM(cio:Map[String,CIO]) extends SjsNode :
  override val name = "PCM"

  def merge(p:PCM):PCM = 
    PCM( cio |+| p.cio)

  override def merge(p: SjsNode): SjsNode = merge(p.asInstanceOf[PCM])


object PCM :      
  def apply(p:GenAst.PCM) :PCM = 
    val i:Map[String,CIO] = p.elements.toList
      .map(x => x.$type -> x)
      .map{(t,o) =>
        t match {
          case "Issues" => t -> Issues.apply(o.asInstanceOf[GenAst.Issues])
          case "Orders" => t -> Orders.apply(o.asInstanceOf[GenAst.Orders])
          case "Clinical" => t -> Clinical.apply(o.asInstanceOf[GenAst.Clinical])
        }

      }.toMap
    PCM(i)

