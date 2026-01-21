package org.aurora.sjsast.yash

import org.aurora.sjsast._

class YashModuleTest extends BaseAsyncTest:
    "YashModule-0 with aliasing" should {
        "convert module to PCM with new alias" in {
            for {
            astPCM <- parse(0)
            module <- Future(Module(astPCM))
            modulePCM = ModulePCM(module)

            _ <- finfo(s"Original module: ${module.name}")
            
            // Convert with alias
            aliasedPCM = modulePCM.toPCM("heart_failure")
            orders = aliasedPCM.cio.get("Orders").get.asInstanceOf[Orders]
            firstRef = orders.ngo.head.ordercoord.head.qurefs.qurc.head
            
            } yield {
                firstRef.refName should be("heart_failure")
            }
        }
    }