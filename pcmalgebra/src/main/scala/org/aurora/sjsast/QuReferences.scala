package org.aurora.sjsast

case class QuReferences(
    qurc: LHSet[QuReference] = LHSet()
) extends AstNode

object QuReferences:
  def apply(qrs: GenAst.QuReferences): QuReferences =
    val refsArray = qrs.quRefs
    val scalaRefs = LHSet.from(refsArray.toSeq.map(QuReference(_)))
    QuReferences(qurc = scalaRefs)