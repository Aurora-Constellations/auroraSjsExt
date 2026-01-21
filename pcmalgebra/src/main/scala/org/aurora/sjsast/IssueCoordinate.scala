package org.aurora.sjsast

import scala.scalajs.js

case class IssueCoordinate(
    name: String,
    narratives: LHSet[NL_STATEMENT] = LHSet(),
    qurefs: LHSet[QuReferences] = LHSet(),
    qu: QU = QU()
)

object IssueCoordinate:
    def apply(ic: GenAst.IssueCoordinate): IssueCoordinate = 
        val name = ic.asInstanceOf[js.Dynamic].selectDynamic("name").toString
        val narratives = LHSet(ic.narrative.toList.map(NL_STATEMENT(_))*)
        val qurefs = LHSet(ic.qurc.toList.map(QuReferences(_))*)
        val qu = QU(ic.qu)
        IssueCoordinate(name, narratives, qurefs, qu)