package org.aurora.sjsast.scoring

import org.aurora.sjsast.BaseAsyncTest
import org.aurora.sjsast.Show.*
import org.aurora.sjsast.meet.separate.Meet.*

class ScoringReferencePCMFileTest extends BaseAsyncTest:

  "A scoring reference PCM" should {
    "be preserved when met with a valid designer PCM" in {
      for
        reference <- ir("reference")
        designer <- ir("designer")
        expected <- ir("expected")
        overlap = reference |&| designer
      yield
        withClue(
          s"""Expected Aurora DSL:
             |${expected.show}
             |Actual Aurora DSL:
             |${overlap.show}
             |""".stripMargin
        ) {
          overlap shouldBe expected
        }
    }
  }
