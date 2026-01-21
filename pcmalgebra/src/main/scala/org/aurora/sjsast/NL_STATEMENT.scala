package org.aurora.sjsast

import scala.scalajs.js

case class NL_STATEMENT(
    name: String
)

object NL_STATEMENT:
  def apply(n: GenAst.NL_STATEMENT): NL_STATEMENT = 
    // Access the 'name' property dynamically to be safe
    val rawName = n.asInstanceOf[js.Dynamic].selectDynamic("name").toString
    NL_STATEMENT(rawName)

  def apply(seq: Seq[GenAst.NL_STATEMENT]): LHSet[NL_STATEMENT] =
    LHSet.from(seq.map(apply))