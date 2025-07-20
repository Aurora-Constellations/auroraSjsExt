package com.axiom.ui.extensionutil

import com.axiom.testutils.*


class SingletonExtensionTest extends AuroraTesting{
    "SingletonExtension" should {
    
        "register and dispatch messages correctly" in {
        val handler = new MessageHandler {
            protected def handleAnyArg(arg: Any): Unit = {
                info(s"Handling message with arg: $arg")   
                arg shouldBe "test message"
            }
        }
    

        SingletonExtension.extensionMessageRouter.registerHandler(MessageStringArg(), handler)
        SingletonExtension.webviewMessageRouter.postMessage(MessageStringArg("test message"))

        }
    }
}
