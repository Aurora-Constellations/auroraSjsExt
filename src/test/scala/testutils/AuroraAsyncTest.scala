package docere.testutils
import org.scalatest._

import wordspec._
import matchers._
import  scala.concurrent.ExecutionContext.Implicits.global

/**
  * Base class for Async testing in Node
  */

class AuroraAsyncTest extends wordspec.AsyncWordSpec with should.Matchers{
  def ppinfo(a:Any) = {
    val s = pprint.apply(a)
    info(s"${s}")
  }
  
  override implicit def executionContext = global

}
