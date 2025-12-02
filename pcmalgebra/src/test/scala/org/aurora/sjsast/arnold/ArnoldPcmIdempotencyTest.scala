package org.aurora.sjsast.arnold

import org.aurora.sjsast._


class ArnoldPcmIdempotencyTest extends BaseAsyncTest:

  "PCM1 + PCM1" should {
    "be PCM1" in { 
      for {
        pcm0      <- parse(0).map{PCM(_)}
        _         <- finfo(s"PCM0: ${pcm0.show}")
        result    <- Future(pcm0 |+| pcm0)
        assertion <- result should be(pcm0)
      } yield assertion
    } 
  } 

  "PCM + empty PCM" should {
    "PCM" in { 
      for {
        pcm0      <- parse(0).map{PCM(_)}
        empty_pcm <- parse(1).map{PCM(_)}   //"blank pcm"
        result    <- Future(pcm0 |+| empty_pcm)
      } yield result should be(pcm0)
    } 
  }



