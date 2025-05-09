
package com.axiom

import testutils.*
class ParserTest extends AuroraAsyncTesting{
  "this" should {
    "work" in {
      val fut = Future(3)
      fut.map(_ + 1).map(_ should be(4))
    }
  }
}
