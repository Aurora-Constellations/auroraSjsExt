package com.axiom.ui.patienttracker

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation._
import com.axiom.ModelFetch
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try
import com.axiom.messaging.*

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
  lazy val vscodeapi: Option[VSCodeApi] = Try(Globals.acquireVsCodeApi()).toOption
}

@JSExportTopLevel("sendRequestToVSCode")
// Function to send Request to VSCode
def sendRequestToVSCode[T <: ToJsObject](request: Request[T]): Unit = {
  val vscode = ScalaJSGlobal.vscodeapi
  vscode.foreach(_.postMessage(request.data.toJsObject(request.command)))
}

@JSExportTopLevel("sendResponseToVSCode")
// Function to send Response to VSCode
def sendResponseToVSCode[R <: ToJsObject](response: Response[R]): Unit = {
  val vscode = ScalaJSGlobal.vscodeapi
  vscode.foreach(_.postMessage(response.result.toJsObject(response.command)))
}

// Function to handle incoming Requests or Responses from VSCode
@JSExportTopLevel("initializeMessageListener")
def initializeMessageListener(): Unit = {
  dom.window.addEventListener("message", { (event: dom.MessageEvent) =>
    val data = event.data.asInstanceOf[js.Dynamic]

    // Optional: check for known source/type if you set it when sending
    val isFromVsCode = !js.isUndefined(data.source) && data.source.toString == "vscode-extension"

    if (isFromVsCode) {
      if(js.isUndefined(data.command)) {
        println("Received message does not contain `command` field.")
      } else {
        val command = data.command.asInstanceOf[String]
        val unitNumber = data.unitNumber.asInstanceOf[String]
        val flag = data.flag.asInstanceOf[String]
        command match {
          case "updateNarratives" =>
            //update to Database
            ModelFetch.addNarrativesFlag(unitNumber, flag).map {
              case Some(_) =>
                println(s"Narrative Flag updated successfully for unit number: ${unitNumber}")
                sendResponseToVSCode(Response("updatedNarratives", UpdatedNarratives(s"Narratives updated successfully for $unitNumber.aurora")))
              case None =>
                println(s"Failed to update Narrative Flag for unit number: ${unitNumber}")
                sendResponseToVSCode(Response("updatedNarratives", UpdatedNarratives(s"Failed to update Narratives updated for $unitNumber.aurora")))
            }
          case other =>
            println(s"Unknown command: $other")
        }
      }
    }
    // If not from VSCode, do nothing (no return needed)
  }: js.Function1[dom.MessageEvent, Unit]) // Explicit JS function type for Scala.js
}