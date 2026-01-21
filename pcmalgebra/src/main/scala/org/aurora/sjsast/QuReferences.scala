package org.aurora.sjsast

import org.aurora.sjsast.{GenAst => G}
import scala.scalajs.js
import scala.collection.mutable.LinkedHashSet

case class QuReferences(
    qurc: LHSet[QuReference] = LHSet()
)

object QuReferences:
  def apply(qrs: G.QuReferences): QuReferences =
    val refsArray = qrs.asInstanceOf[js.Dynamic].selectDynamic("quRefs").asInstanceOf[js.Array[G.QuReference]]
    val scalaRefs = LinkedHashSet.from(refsArray.toSeq.map(QuReference(_)))
    QuReferences(qurc = scalaRefs)