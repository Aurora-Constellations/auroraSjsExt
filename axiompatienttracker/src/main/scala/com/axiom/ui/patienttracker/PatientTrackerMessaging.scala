package com.axiom.ui.patienttracker

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation._
import com.axiom.ModelFetch
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try
import com.axiom.vscode.ScalaJSGlobal
import com.axiom.messaging.*

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
      CommandDispatcher.dispatch(data)
    }
    // If not from VSCode, do nothing (no return needed)
  }: js.Function1[dom.MessageEvent, Unit]) // Explicit JS function type for Scala.js
}