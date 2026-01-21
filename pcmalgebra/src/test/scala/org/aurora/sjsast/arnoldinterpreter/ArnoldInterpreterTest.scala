package org.aurora.sjsast.arnoldinterpreter

import org.aurora.sjsast._

class ArnoldInterpreterTest extends BaseAsyncTest:

  "evaluate" should {
    "5+3 should be 8" in { 
      for {
        result     <- Future(SimpleInterpreter.evaluate(Add(Number(5), Number(3))))
      } yield result should be(IntValue(8))
    }

    "2 should be < 5" in {
      for {
        result     <- Future(SimpleInterpreter.evaluate(LessThan(Number(2), Number(5))))
      } yield result should be(BoolValue(true))
    }

    "2 + 2 should be less than 5" in {
      for {
        result     <- Future(SimpleInterpreter.evaluate(LessThan(Add(Number(2),Number(2)), Number(5))))
      } yield result should be(BoolValue(true))
    }
  }