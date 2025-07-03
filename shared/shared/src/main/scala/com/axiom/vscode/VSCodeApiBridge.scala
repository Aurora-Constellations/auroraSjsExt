package com.axiom.vscode

import scala.scalajs.js
import scala.scalajs.js.annotation._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

/** Facade for the VS Code WebView API's messaging interface. Allows the Scala.js app to post messages to the VS Code
  * extension backend.
  */
@js.native
trait VSCodeApi extends js.Object {

  /** Sends a message to the VS Code extension host from the WebView.
    *
    * @param message
    *   The message to be sent (can be any JS-compatible object).
    */
  def postMessage(message: js.Any): Unit = js.native
}

/** Facade for accessing global JavaScript functions in the browser context. Specifically exposes the VS Code API
  * acquisition method.
  */
@js.native
@JSGlobalScope
object Globals extends js.Object {

  /** Acquires the VS Code API instance available in a WebView context.
    *
    * @return
    *   A VSCodeApi object for communication with the extension backend.
    */
  def acquireVsCodeApi(): VSCodeApi = js.native
}

/** Singleton object that lazily and safely acquires the VS Code API instance. Wraps the call to acquireVsCodeApi in a
  * Try to prevent runtime errors if the API is not available (e.g., outside a VS Code WebView).
  */
object ScalaJSGlobal {

  /** Optionally contains the VS Code API instance, if it could be acquired.
    */
  lazy val vscodeapi: Option[VSCodeApi] = Try(Globals.acquireVsCodeApi()).toOption
}
