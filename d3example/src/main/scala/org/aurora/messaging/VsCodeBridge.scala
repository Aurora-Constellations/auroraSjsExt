package org.aurora.messaging

import com.axiom.protocol.{ToExtension, ToWebview, Wire}
import org.scalajs.dom
import scala.scalajs.js

/** The webview's only door to the extension host: typed [[ToExtension]] out, typed [[ToWebview]] in.
  *
  * Owns the one-and-only `acquireVsCodeApi()` call (VS Code throws if a webview acquires it twice), so nothing else
  * in the webview may call it.
  *
  * On the wire a message is a plain JS object tagged by `command`; the (de)serialisation to and from the protocol
  * types is done by [[Wire]].
  */
object VsCodeBridge {

  // Only available inside a VS Code webview; absent when opened in a plain browser (e.g. the Vite dev server)
  private lazy val api: Option[js.Dynamic] =
    if (js.typeOf(js.Dynamic.global.acquireVsCodeApi) == "function") Some(js.Dynamic.global.acquireVsCodeApi())
    else None

  def send(msg: ToExtension): Unit =
    api.foreach(_.postMessage(js.JSON.parse(Wire.encode(msg))))

  /** Registers `handler` for every well-formed message from the extension host; anything else is logged and dropped. */
  def listen(handler: ToWebview => Unit): Unit =
    dom.window.addEventListener(
      "message",
      (event: dom.MessageEvent) => {
        val raw = event.data.asInstanceOf[js.Any]
        if (!js.isUndefined(raw)) {
          Wire.decodeToWebview(js.JSON.stringify(raw)) match {
            case Right(msg) => handler(msg)
            case Left(error) => dom.console.warn(s"Ignoring unrecognised message from extension: $error")
          }
        }
      }
    )
}
