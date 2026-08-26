package org.aurora.sjsast.arnold

import org.scalatest._
import wordspec._
import matchers._
import org.aurora.sjsast._

class ArnoldJoinMeetAsyncTest extends BaseAsyncTest :
  "this" should {
    "join" in {
      import JoinMeet.{given,*}
      import Show.{given,*}

      val f1 = testfilepath(0)
      info(s"Testing join on file: $f1")


      for{
        pcm1 <- ir(0)
        pcm2 <- ir(0)
        _     <- finfo(s"$pcm1")
        result <- pcm1 |+| pcm2 should be( pcm1)
      }
      yield result


   }
  }
