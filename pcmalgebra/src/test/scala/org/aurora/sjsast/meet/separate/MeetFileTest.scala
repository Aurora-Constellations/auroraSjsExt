package org.aurora.sjsast.meet.separate

import org.aurora.sjsast.{BaseAsyncTest, PCM}
import org.aurora.sjsast.Show.*
import org.aurora.sjsast.meet.separate.Meet.*

class MeetFileTest extends BaseAsyncTest:

  // Run from the repository root:
  //
  // Only this suite:
  //   sbt 'pcmalgebra/Test/testOnly org.aurora.sjsast.meet.separate.MeetFileTest'
  //
  // This suite and MeetTest together:
  //   sbt 'pcmalgebra/Test/testOnly org.aurora.sjsast.meet.separate.MeetTest org.aurora.sjsast.meet.separate.MeetFileTest'
  //
  // One scenario selected by a substring of its test name:
  //   sbt 'pcmalgebra/Test/testOnly org.aurora.sjsast.meet.separate.MeetFileTest -- -z "qualifier references"'

  // compares pcm values and shows readable dsl when they differ
  private def assertPcm(label: String, actual: PCM, expected: PCM) =
    withClue(
      s"""$label
         |Expected Aurora DSL:
         |${expected.show}
         |Actual Aurora DSL:
         |${actual.show}
         |""".stripMargin
    ) {
      actual shouldBe expected
    }

  // loads one fixture trio and checks its meet behavior
  private def meetCase(leftName: String, rightName: String, expectedName: String) =
    for
      // identifies the input and expected fixture files
      _ <- finfo(
        s"Fixtures: MeetFile-$leftName.aurora |&| MeetFile-$rightName.aurora -> MeetFile-$expectedName.aurora"
      )

      // parses the aurora files into pcm values
      left <- ir(leftName)
      right <- ir(rightName)
      expected <- ir(expectedName)

      // calculates the expected direction and its reverse
      result = left |&| right
      reverseResult = right |&| left

      // calculates both self meets for idempotence
      leftSelfResult = left |&| left
      rightSelfResult = right |&| right

      // logs the readable aurora inputs
      _ <- finfo(s"Left Aurora DSL:\n${left.show}")
      _ <- finfo(s"Right Aurora DSL:\n${right.show}")
    yield
      // checks the expected intersection and commutativity
      assertPcm(s"$leftName meet $rightName", result, expected)
      assertPcm(s"$rightName meet $leftName", reverseResult, expected)

      // checks idempotence for both inputs
      assertPcm(s"$leftName meet itself", leftSelfResult, left)
      assertPcm(s"$rightName meet itself", rightSelfResult, right)

  // file-based meet scenarios
  "Meet parsed Aurora files" should {
    "intersect whole PCM sections, groups, coordinates, values, and narratives" in {
      meetCase("whole-left", "whole-right", "whole-expected")
    }

    "intersect qualifier references by reference name" in {
      meetCase("references-left", "references-right", "references-expected")
    }

    "intersect order-group qualifiers without changing their representation" in {
      meetCase("qualifiers-left", "qualifiers-right", "qualifiers-expected")
    }

    "intersect same-name clinical items regardless of value presence" in {
      meetCase("variants-left", "variants-right", "variants-expected")
    }
  }
