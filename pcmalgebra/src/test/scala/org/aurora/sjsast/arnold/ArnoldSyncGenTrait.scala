package org.aurora.sjsast.arnold
import org.scalatest._

import wordspec._
import matchers._
import org.aurora.sjsast.OrderCoordinate
export org.scalacheck.Gen
trait ArnoldSyncGenTrait extends  wordspec.AnyWordSpec with should.Matchers with org.scalatestplus.scalacheck.ScalaCheckPropertyChecks :
  def ocoord(count: Int) : Seq[OrderCoordinate] =
      (1 to count).map{i => OrderCoordinate(s"oc$i",narrative(3),org.aurora.sjsast.QuReferences(Set.empty))}

  def narrative(count:Int ) : Set[String] =
      Gen.choose(0,count) 
      (0 to count).map{i => s"-narrative$i;"}.toSet