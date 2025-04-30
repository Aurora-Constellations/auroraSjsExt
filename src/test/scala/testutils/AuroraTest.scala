package docere.testutils
import org.scalatest._
import wordspec._
import matchers._


class AuroraTest extends AnyWordSpec with should.Matchers{
  def ppinfo(a:Any) = {
    val s = pprint.apply(a)
    info(s"${s}")
  }


  




}
