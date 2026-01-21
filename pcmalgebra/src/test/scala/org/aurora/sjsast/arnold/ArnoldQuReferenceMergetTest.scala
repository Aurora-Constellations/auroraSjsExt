package org.aurora.sjsast.arnold

import org.aurora.sjsast._
import org.aurora.sjsast.JoinMeet.*

class ArnoldQuReferenceMergeTest extends BaseAsyncTest:

  "Merging QuReference" should {
    "combine the qualifiers" in { 

      val quRef1 = QuReference("r1", QU(LHSet('~')))
      val quRef2 = QuReference("r1", QU(LHSet('!')))

      for {
        quResult   <- Future(quRef1 |+| quRef2)
        assertion          <- quResult should be(QuReference("r1", QU(LHSet('~','!'))))
      } yield assertion
    } 
  }