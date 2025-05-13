package com.axiom.ui.patienttracker

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation._

// TODO: Make Case Class for Message and Payloads

@js.native
trait VSCodeApi extends js.Object {
  def postMessage(message: js.Any): Unit = js.native
}

@js.native
@JSGlobalScope
object Globals extends js.Object {
  def acquireVsCodeApi(): VSCodeApi = js.native
}

//Converting Facade to a singleton object
object ScalaJSGlobal {
  lazy val vscodeapi = acquireVsCodeApi()
  private def acquireVsCodeApi(): VSCodeApi = Globals.acquireVsCodeApi()
}

@JSExportTopLevel("sendMessageToVSCode")
def sendMessageToVSCode(command: String, filename: String): Unit = {
  val vscode = ScalaJSGlobal.vscodeapi
  if (command.nonEmpty || filename.nonEmpty) {
    vscode.postMessage(js.Dynamic.literal(
      command = command,
      filename = filename
    ))
  } else {
    println("Command or filename is empty. Not sending message.")
  }
}

// Function to handle incoming messages
@JSExportTopLevel("initializeMessageListener")
def initializeMessageListener(): Unit = {
  dom.window.addEventListener("message", { (event: dom.MessageEvent) =>
    val data = event.data.asInstanceOf[js.Dynamic]
    
    if(js.isUndefined(data.command)) {
      println("Received message does not contain `command` field.")
    } else {
      println("Raw message received:")
      val command = data.command.asInstanceOf[String]
      command match {
        case "updateNarratives" =>
          println(s"updateNarratives: ${js.JSON.stringify(data)}") // helps debugging
          sendMessageToVSCode("updatedNarratives", "Narratives updated successfully")
        case other =>
          println(s"Unknown command: $other")
      }
    }
  })
}