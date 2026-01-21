package org.aurora.sjsast.arnold


class ArnoldGenTest extends ArnoldSyncGenTrait :
  "this" should {
    "work" in {
      forAll(Gen.choose(0,5)) { c =>
        val oc = ocoord(c)       
        info(s"$oc")
        true should be(true)
      }
  }}
