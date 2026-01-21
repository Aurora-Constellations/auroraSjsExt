package org.aurora.sjsast.arnold

import org.aurora.sjsast._
import org.aurora.sjsast.JoinMeet.*

class ArnoldQuReferenceMergeTest extends BaseAsyncTest:

  "Merging QuReference" should {
    "combine the qualifiers" in { 

      val quRef1 = QuReference(QU(LHSet('~')), "r1")
      val quRef2 = QuReference(QU(LHSet('!')), "r1")

      for {
        quResult   <- Future(quRef1 |+| quRef2)
        assertion          <- quResult should be(QuReference(QU(LHSet('~','!')), "r1"))
      } yield assertion
    } 
  }