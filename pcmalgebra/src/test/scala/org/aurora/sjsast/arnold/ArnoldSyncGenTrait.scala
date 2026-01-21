package org.aurora.sjsast.arnold

import org.scalatest._
import wordspec._
import matchers._
import org.aurora.sjsast._

export org.scalacheck.Gen

trait ArnoldSyncGenTrait extends wordspec.AnyWordSpec with should.Matchers with org.scalatestplus.scalacheck.ScalaCheckPropertyChecks:
  
  def ocoord(count: Int): Seq[OrderCoordinate] =
    (1 to count).map { i => 
      OrderCoordinate(
        name = s"oc$i",
        narratives = narratives(3),
        qurefs = LHSet(QuReferences())
      )
    }

  def narratives(count: Int): LHSet[NL_STATEMENT] =
    LHSet.from(
      (0 to count).map { i => NL_STATEMENT(s"??narrative$i;") }
    )
  
  // Helper to create QuReferences with specific references
  def quReferences(refs: (String, String)*): LHSet[QuReferences] =
    val quRefs = refs.map { case (refName, quStr) =>
      QuReference(
        refName = refName,
        qu = QU(query = LHSet.from(quStr.toSeq))
      )
    }
    LHSet(QuReferences(LHSet.from(quRefs)))
  
  // Helper to create a single QuReference
  def quReference(refName: String, quStr: String = ""): QuReference =
    QuReference(
      refName = refName,
      qu = QU(query = LHSet.from(quStr.toSeq))
    )
  
  // Helper to create OrderCoordinate with references
  def ocoordWithRefs(name: String, refs: (String, String)*): OrderCoordinate =
    OrderCoordinate(
      name = name,
      narratives = narratives(2),
      qurefs = quReferences(refs*)
    )