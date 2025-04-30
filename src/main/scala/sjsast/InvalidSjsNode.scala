package docere.sjsast

case class InvalidSjsNode() extends SjsNode :
    override val name = "InvalidSjsNode"
    override def merge(p: SjsNode): SjsNode = this
