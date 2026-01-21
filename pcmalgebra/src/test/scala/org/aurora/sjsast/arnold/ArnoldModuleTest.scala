package org.aurora.sjsast.arnold

import org.aurora.sjsast._

class ArnoldModuleTest extends BaseAsyncTest:

  "ArnoldModule-0" should {
    "be a module" in { 

      for {
        astPCM      <- parse(0)
        modulePCM   <- Future(ModulePCM(Module(astPCM)))
      } yield modulePCM.module.name should be("moduleA")
    } 
  }