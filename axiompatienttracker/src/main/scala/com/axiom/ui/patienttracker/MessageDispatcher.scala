package com.axiom.ui.patienttracker

import com.axiom.messaging.MessagingJson.*
import com.axiom.messaging.*
import org.scalajs.dom
import scala.scalajs.js
import _root_.com.axiom.ui.patienttracker.EventChannels

object MessageDispatcher {

  def start(): Unit = {
    EventChannels.outgoing.subscribe {
      case req: Request[CreateAuroraFile] @unchecked if req.command == "createAuroraFile" =>
        post(encode(req))
      case req: Request[OpenAuroraFile] @unchecked if req.command == "openAuroraFile" =>
        post(encode(req))
      case res: Response[AddedToDB] @unchecked if res.command == "addedToDB" =>
        post(encode(res))
      case res: Response[UpdateNarratives] @unchecked if res.command == "updatedNarratives" =>
        post(encode(res))
      case req: Request[_] =>
        println(s"[Webview] Unsupported request: ${req.command}")
      case res: Response[_] =>
        println(s"[Webview] Unsupported response: ${res.command}")
      case _ =>
        println("[Webview] Received unknown message.")
    }
  }

  private def post(json: String): Unit = {
    val vscode = js.Dynamic.global.acquireVsCodeApi()
    vscode.postMessage(json)
    println(s"[Webview] Sent: $json")
  }
}
