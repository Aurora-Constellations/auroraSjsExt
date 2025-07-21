package com.axiom

import scala.concurrent.Future
import org.scalatest._
import wordspec._
import matchers._
import com.raquo.airstream.ownership.ManualOwner


object testutils :
  export scala.concurrent.Future

  class AuroraAsyncTesting extends wordspec.AsyncWordSpec with should.Matchers :
    implicit override def executionContext = org.scalajs.macrotaskexecutor.MacrotaskExecutor

  class AuroraTesting    extends AnyWordSpec with should.Matchers


  class SjsTesting    extends AnyWordSpec with should.Matchers

  class LaminarWordSpecTesting extends wordspec.AnyWordSpec with  BeforeAndAfter with should.Matchers :
    given owner: ManualOwner = new ManualOwner()
    before {    }

    after{
      owner.killSubscriptions()
    }
  