package org.aurora.sjsast
 
import scala.collection.mutable.LinkedHashSet
case class ClinicalValue (name :String, narrative:LinkedHashSet[NL_STATEMENT]=LinkedHashSet.empty, qurefs:QuReferences)