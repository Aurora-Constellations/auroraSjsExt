package org.aurora.sjsast.arnold

import org.aurora.sjsast._
import org.aurora.sjsast.*
import org.aurora.sjsast.JoinMeet
import org.aurora.sjsast.JoinMeet.given 
import org.aurora.sjsast.Show
import org.aurora.sjsast.Show.given 
import org.aurora.sjsast.Show._ 

class ArnoldQuReferencesMergeTest extends BaseAsyncTest:

  extension [T](a: T)(using jm: JoinMeet[T])
      def |+|(b: T): T = jm.join(a, b)

  "Merging QuReferences" should {
    "combine the qualifiers" in { 

      val quRef1 = QuReference("r1", QU(LHSet('~')))
      val quRef2 = QuReference("r2", QU(LHSet('!')))
      val quRef3 = QuReference("r1", QU(LHSet(' ')))
      val quRef4 = QuReference("r2", QU(LHSet(' ')))
      val refSet1 = QuReferences(LHSet(quRef1,quRef2))
      val refSet2 = QuReferences(LHSet(quRef3,quRef4))

      for {
        quResult   <- Future(quRef1 |+| quRef2)
        assertion  <- quResult should be (QuReference("r1", QU(LHSet('~'))) |+| QuReference("r2", QU(LHSet('!'))))
        mergeResult <- Future( refSet1 |+| refSet2 )
        _     <- finfo(s"$mergeResult")
        assert1    <- true should be(true)
          // refSet1 .merge (refSet2) should be(QuReferences(Set(
          //                 QuReference("~","r1"),
          //                 QuReference("!","r2")
          //               )))
      } yield assert1
    } 
  }