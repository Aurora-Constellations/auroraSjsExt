package org.aurora.sjsast.arnold

import org.aurora.sjsast._

class ArnoldQuReferenceMergeTest extends BaseAsyncTest:

  "Merging QuReference" should {
    "combine the qualifiers" in { 

      val quRef1 = QuReference("~","r1")
      val quRef2 = QuReference("!","r1")

      for {
        quResult   <- Future(quRef1.merge(quRef2))
        assertion          <- quResult should be(QuReference("~!","r1"))
      } yield assertion
    } 
  }