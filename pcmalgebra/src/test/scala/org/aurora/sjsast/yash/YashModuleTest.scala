package org.aurora.sjsast.yash

import org.aurora.sjsast._

class YashModuleTest extends BaseAsyncTest:
    "YashModule-0 with aliasing" should {
        "convert module to PCM with new alias" in {
            for {
            astPCM <- parse(0)
            modulePCM = ModulePCM(astPCM)

            _ <- finfo(s"Original module: ${modulePCM.name}")
            
            // Convert with alias
            aliasedPCM = modulePCM.toPCM("heart_failure")
            orders = aliasedPCM.cio.get("Orders").get.asInstanceOf[Orders]
            firstRef = orders.ngo.head.orderCoordinates.head.refs.refs.head
            
            } yield {
                firstRef.name should be("heart_failure")
            }
        }
    }