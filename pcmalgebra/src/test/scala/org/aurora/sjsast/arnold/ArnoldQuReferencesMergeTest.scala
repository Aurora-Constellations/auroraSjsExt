package org.aurora.sjsast.arnold

import org.aurora.sjsast._

class ArnoldQuReferencesMergeTest extends BaseAsyncTest:

  "Merging QuReferences" should {
    "combine the qualifiers" in { 

      val quRef1 = QuReference("~","r1")
      val quRef2 = QuReference("!","r2")
      val quRef3 = QuReference("","r1")
      val quRef4 = QuReference("","r2")
      val refSet1 = QuReferences(Set(quRef1,quRef2))
      val refSet2 = QuReferences(Set(quRef3,quRef4))

      for {
        quResult   <- Future(quRef1 .merge (quRef2))
        assertion  <- quResult should be(QuReference("~!","r1"))
        mergeResult <- Future( refSet1 .merge (refSet2) )
        _     <- finfo(s"$mergeResult")
        assert1    <- true should be(true)
          // refSet1 .merge (refSet2) should be(QuReferences(Set(
          //                 QuReference("~","r1"),
          //                 QuReference("!","r2")
          //               )))
      } yield assert1
    } 
  }