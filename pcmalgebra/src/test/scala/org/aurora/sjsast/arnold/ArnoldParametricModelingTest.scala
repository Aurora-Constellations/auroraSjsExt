package org.aurora.arnold

import org.scalatest._
import wordspec._
import matchers._
import org.aurora.sjsast.BaseAsyncTest

export  org.aurora.utils.fileutils


class ArnoldParametricModelingTest extends BaseAsyncTest {
  s"Test file 0" should {
    "work" in {
      val testFile = testfilepath(0)

      for{
        _         <-  finfo(s"${tempoutputpath("temp.txt")}")
        _         <- finfo(s"$testFile")
        pcm       <- optIr(0)
        _         <-  finfo(s"${pcm.get}")
        text      <- fshow(pcm.get)
        _         <- falert(s"$text")
        _         <- Future ( fileutils.writeFileSync(tempoutputpath("temp.txt"), text) )

        result      <- pcm should be (pcm) //TODO this is a placeholder test, need to write actual tests for the PCM content
      } yield (result)

    }
  }
}
