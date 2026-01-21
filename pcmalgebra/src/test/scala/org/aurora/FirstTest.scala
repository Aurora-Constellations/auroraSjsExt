package org.aurora

import org.scalatest._
import wordspec._
import matchers._

export  org.aurora.utils.fileutils


class FirstTest extends AnyWordSpec with should.Matchers{
  "this" should {
    "work" in {
      info(fileutils.cwd)
      info(fileutils.testResourcesPath)

      true should be(true)
    }
  }
}
