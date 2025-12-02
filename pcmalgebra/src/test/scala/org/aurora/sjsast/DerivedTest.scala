package org.aurora.sjsast

import scala.concurrent.Future


class DerivedTest extends BaseAsyncTest :

  "this" should {
    "work" in {

      info(testfilepath(0))
      val fut = Future(3)
      fut.map(_ + 1).map(_ should be(4))
    }
  }

