package com.axiom.ui.extensionutil

import com.axiom.testutils.*


class SingletonExtensionTest extends LaminarWordSpecTesting{
    "SingletonExtension" should {
        "register and handle a message" in {
            info("Testing SingletonExtension message handling")

            // external state that will be modified by StringMessageHandler
            var stateString = ""

            class StringMessageHandler extends MessageHandler {
                protected def handleAnyArg(arg: Any): Unit = {
                    handleStringArg(arg.asInstanceOf[String])
                }
                def handleStringArg(strArg: String) = {
                    info(s"Handling string message: $strArg")
                    stateString = strArg
                }
            }
        
                
            val handler = new StringMessageHandler ()
        

            SingletonExtension.extensionMessageRouter.registerHandler(MessageStringArg(), handler)
            SingletonExtension.webviewMessageRouter.postMessage(MessageStringArg("test message"))
            stateString shouldBe "test message"

        }
    }
}