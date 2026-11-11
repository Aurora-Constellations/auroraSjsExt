import org.scalatest._

import wordspec._
import matchers._
import org.aurora.visual.LabelWrap

class LabelWrapTest extends wordspec.AnyWordSpec with should.Matchers {
  "LabelWrap.lines" should {
    "keep a short label on one line" in {
      LabelWrap.lines("congestive_Heart_Failure") should be (List("congestive_Heart_Failure"))
    }
    "wrap a long label at spaces, no line longer than the limit" in {
      val text = "?? correlate this derived score with bleeding risk contraindications and local guidance before acting;"
      val lines = LabelWrap.lines(text)
      lines.size should be > 1
      all(lines.map(_.length)) should be <= LabelWrap.MaxLineChars
      lines.mkString(" ") should be (text)
    }
    "break a long underscored name after an underscore, not mid-word" in {
      LabelWrap.lines("review_documented_stroke_risk_factors") should be (List("review_documented_stroke_risk_", "factors"))
    }
    "split a single word longer than a line" in {
      LabelWrap.lines("a" * 70) should be (List("a" * 32, "a" * 32, "a" * 6))
    }
  }

  "LabelWrap sizes" should {
    "match the old size for one-line labels" in {
      val lines = LabelWrap.lines("baseline_note")
      LabelWrap.width(lines) should be ("baseline_note".length * 7 + 20.0)
      LabelWrap.height(lines) should be (40.0)
    }
    "grow taller, not wider, for wrapped labels" in {
      val lines = LabelWrap.lines("?? derived high CHA2DS2_VASc stroke risk from the currently explicit AF risk factors;")
      LabelWrap.width(lines) should be <= (LabelWrap.MaxLineChars * 7 + 20.0)
      LabelWrap.height(lines) should be (40.0 + (lines.size - 1) * 16.0)
    }
  }
}
