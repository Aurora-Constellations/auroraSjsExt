package com.axiom.ui.patienttracker

import com.axiom.messaging.MessagingJson.*
import com.axiom.messaging.*
import org.scalajs.dom
import scala.scalajs.js

object MessageListener {

  def start(): Unit = {
    dom.window.addEventListener("message", { (event: dom.MessageEvent) =>
      val raw = event.data.toString
      println(s"[Webview] Received: $raw")

      decodeUpdateNarratives(raw).foreach(EventChannels.incoming.publish)
      decodeCreateAuroraFile(raw).foreach(EventChannels.incoming.publish)
      decodeOpenAuroraFile(raw).foreach(EventChannels.incoming.publish)
      decodeAddedToDB(raw).foreach(EventChannels.incoming.publish)
    }: js.Function1[dom.MessageEvent, Unit])
  }
}
