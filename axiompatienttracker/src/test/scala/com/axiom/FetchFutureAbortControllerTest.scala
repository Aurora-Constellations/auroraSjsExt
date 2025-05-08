package com.axiom
import testutils._

import scala.concurrent.Future

class FetchFutureAbortControllerTest extends AuroraAsyncTesting {

  "this" should {
    "work" in {
        true should be(true)

        for{
          a <- Future{1+1}
          b <- Future{2+2}
          
        } yield  {
          info(s"result = ${a*b}")
          true should be(true)
        }
    }
  }
}
