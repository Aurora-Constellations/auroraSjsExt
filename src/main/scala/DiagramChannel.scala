package com.axiom.visual

import com.axiom.protocol.{ToExtension, ToWebview, Wire}
import scala.scalajs.js
import typings.vscode.mod as vscode

/** The extension host's side of the diagram webview's wire: typed [[ToWebview]] out, typed [[ToExtension]] in.
  *
  * On the wire a message is a plain JS object tagged by `command`; the (de)serialisation to and from the protocol
  * types is done by [[Wire]].
  */
object DiagramChannel {

  def post(webview: vscode.Webview, msg: ToWebview): Unit = {
    webview.postMessage(js.JSON.parse(Wire.encode(msg)))
    ()
  }

  /** Decodes a raw `onDidReceiveMessage` payload; anything that is not a protocol message is a `Left`. */
  def decode(raw: Any): Either[String, ToExtension] =
    Wire.decodeToExtension(js.JSON.stringify(raw.asInstanceOf[js.Any]))
}
