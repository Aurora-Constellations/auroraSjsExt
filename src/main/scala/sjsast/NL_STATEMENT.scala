package docere.sjsast

case class NL_STATEMENT(val name: String) extends SjsNode:
  override def merge(p: SjsNode): SjsNode = p

// Can drop the apply method entirely because Scala case classes already provide an apply with String:
// object Narrative:
//   def apply(n: String): NL_STATEMENT =
//     NL_STATEMENT(n)