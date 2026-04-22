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
  type ClinicalItem                 = GenAstMod.ClinicalItem
  type QU                           = GenAstMod.QU
  type QuReference                  = GenAstMod.QuReference
  type QuReferences                 = GenAstMod.QuReferences
  type NL_STATEMENT                 = GenAstMod.NL_STATEMENT
  type LangiumReference[T]          = Reference[T]
  type SingleValueUnit              = GenAstMod.SingleValueUnit
  type Module                       = GenAstMod.MODULE
  type ReferenceCoordinate          = GenAstMod.ReferenceCoordinate
  type ArithModule                  = GenAstMod.ArithModule
  type Evaluation                   = GenAstMod.Evaluation
  type Expression                   = GenAstMod.Expression
  type OrderElement                 = GenAstMod.OrderElement