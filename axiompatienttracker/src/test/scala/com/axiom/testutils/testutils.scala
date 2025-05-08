package com.axiom

import scala.concurrent.Future
import org.scalatest._
import wordspec._
import matchers._


object testutils :
  export scala.concurrent.Future

  class AuroraAsyncTesting extends wordspec.AsyncWordSpec with should.Matchers :
    implicit override def executionContext = org.scalajs.macrotaskexecutor.MacrotaskExecutor

  class AuroraTesting    extends AnyWordSpec with should.Matchers