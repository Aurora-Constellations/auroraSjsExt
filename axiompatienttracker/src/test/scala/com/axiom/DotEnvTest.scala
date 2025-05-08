package com.axiom

import testutils.*


class DotEnvTest extends AuroraTesting :
  "properties from .env" should {
    "be accessible" in {
      import scala.scalajs.js

      val myenv = js.Dynamic.global.process.env.selectDynamic("myenv").asInstanceOf[js.UndefOr[String]]
      myenv should be ("arnold")

      val hello = js.Dynamic.global.process.env.selectDynamic("hello").asInstanceOf[js.UndefOr[String]]
      hello should be ("world")

    }
  }
