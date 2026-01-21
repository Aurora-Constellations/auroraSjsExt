package org.aurora.sjsast

import scala.concurrent.Future

import typings.auroraLangium.cliMod.{getEmptyAuroraServices, extractAstNode, getAuroraServices}
import typings.auroraLangium.cliMod.extractAstNode
import scala.scalajs.js
import typings.langium.langiumStrings.langium
import org.aurora.sjsast.JoinMeet.*
import org.aurora.sjsast.Show.*

class ParseTest extends BaseAsyncTest:
  lazy val emptyServices = getAuroraServices()
  // def parse1PCM(filename:String) =  
  
  

  "Parse" should {
    "parse file correctly" in { 
      //TODO note if there is no file named Parse-0.aurora, it will automatically create one
      //SHOULD TEST FILE NAMING BE MORE DESCRIPTIVE e.g. Parse-Valid.aurora

      for {
        pcm0 <- parse(0)
        pcm1 <- parse(1)
         _     <- Future(info(s"pcm0: $pcm0"))
         _     <- Future(info(s"pcm1: $pcm1"))
        b <- Future(true should be (true))
      } yield {
        b
      }
    }



    "parse(1) correctly" in { 
      val path = testfilepath(1)

      for {
        result <- parse(1)
         _     <- Future(info(s"result: $result"))
        //TODO HOW DO YOU REPRESENT CATCHING EXCEPTION 
        b <- Future(true should be (true))


      } yield {
        b
      }
    }

    "pcm+pcm" in {
      val path0 = testfilepath(0)
      import JoinMeet.given
      import org.aurora.sjsast.Show.given

      for {
        langiumPCM <- org.aurora.utils.fileutils.parse(path0).toFuture
        pcm:PCM <- Future( PCM(langiumPCM))
        result   <- Future(pcm |+| pcm)
        _       <- Future(info(s"result: ${result.show}"))
        b <- Future(result should be (pcm))
      } yield {
        b
        
      }
    }

} 