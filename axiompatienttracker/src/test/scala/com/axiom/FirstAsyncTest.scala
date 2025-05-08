
package com.axiom

import testutils.*

class FirstAsyncTest extends AuroraAsyncTesting{
  implicit override def executionContext = org.scalajs.macrotaskexecutor.MacrotaskExecutor
  "this" should {
    "work" in {
      val fut = Future(3)
      fut.map(_ + 1).map(_ should be(4))
    }
  }
}
