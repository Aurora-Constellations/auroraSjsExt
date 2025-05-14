package com.axiom.ui.patienttracker

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation._
import com.axiom.ModelFetch
import scala.concurrent.ExecutionContext.Implicits.global


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
      val unitNumber = data.unitNumber.asInstanceOf[String]
      val flag = data.flag.asInstanceOf[String]
      command match {
        case "updateNarratives" =>
          println(s"updateNarratives: ${flag}") // helps debugging
          //update to Database
          ModelFetch.addNarrativesFlag(unitNumber, flag).map {
            case Some(_) =>
              println(s"Narrative Flag updated successfully for unit number: ${unitNumber}")
            case None =>
              println(s"Failed to update Narrative Flag for unit number: ${unitNumber}")
          }
          sendMessageToVSCode("updatedNarratives", s"Narratives updated successfully for $unitNumber.aurora")
        case other =>
          println(s"Unknown command: $other")
      }
    }
  })
}