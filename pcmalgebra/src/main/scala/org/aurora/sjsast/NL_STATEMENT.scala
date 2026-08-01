package org.aurora.sjsast

case class NL_STATEMENT(
    name: String
) extends AstNode

object NL_STATEMENT:
  def apply(n: GenAst.NL_STATEMENT): NL_STATEMENT = 
    val rawName = n.name
    NL_STATEMENT(rawName)

  def apply(seq: Seq[GenAst.NL_STATEMENT]): LHSet[NL_STATEMENT] =
    LHSet.from(seq.map(apply))