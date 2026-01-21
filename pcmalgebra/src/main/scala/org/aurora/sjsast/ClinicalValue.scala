package org.aurora.sjsast

case class ClinicalValue(
    name :String, 
    narrative:LHSet[NL_STATEMENT] = LHSet(), 
    qurefs:LHSet[QuReferences] = LHSet()
)