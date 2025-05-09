package com.axiom.ui.patienttracker

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation._

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
  vscode.postMessage(js.Dynamic.literal(
    command = command,
    filename = filename
  ))
}