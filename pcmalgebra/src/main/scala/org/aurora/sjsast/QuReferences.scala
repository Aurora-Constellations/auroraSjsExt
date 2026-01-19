package org.aurora.sjsast

import org.aurora.sjsast.{GenAst => G}
import scala.scalajs.js
import scala.collection.mutable.LinkedHashSet

case class QuReferences(
    refs: LHSet[QuReference] = LHSet()
)

object QuReferences:
  def fromJs(qrs: G.QuReferences): QuReferences =
    val refsArray = qrs.asInstanceOf[js.Dynamic].selectDynamic("refs").asInstanceOf[js.Array[G.QuReference]]
    val scalaRefs = LinkedHashSet.from(refsArray.toSeq.map(QuReference.fromJs))
    QuReferences(refs = scalaRefs)