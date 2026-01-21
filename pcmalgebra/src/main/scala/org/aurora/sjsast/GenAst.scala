package org.aurora.sjsast

object GenAst :
  import typings.auroraLangium.distTypesSrcLanguageGeneratedAstMod as GenAstMod
  import typings.langium.libSyntaxTreeMod.Reference
  
  type PCM                          = GenAstMod.PCM
  type Issues                       = GenAstMod.Issues
  type Orders                       = GenAstMod.Orders
  type Clinical                     = GenAstMod.Clinical
  type IssueCoordinate              = GenAstMod.IssueCoordinate
  type NGO                          = GenAstMod.NamedGroupOrder
  type NGC                          = GenAstMod.NamedGroupClinical
  type OrderCoordinate              = GenAstMod.OrderCoordinate
  type ClinicalCoordinate           = GenAstMod.ClinicalCoordinate
  type ClinicalValue                = GenAstMod.ClinicalValue
  type ClinicalCoordinateValue      = GenAstMod.ClinicalCoordinate | GenAstMod.ClinicalValue
  type RefCoordinateType            = GenAstMod.ReferenceCoordinate
  type QU                           = GenAstMod.QU
  type QuReference                  = GenAstMod.QuReference
  type QuReferences                 = GenAstMod.QuReferences
  type NL_STATEMENT                 = GenAstMod.NL_STATEMENT
  type LangiumReference[T]          = Reference[T]
  
  type Module                       = GenAstMod.MODULE
  

