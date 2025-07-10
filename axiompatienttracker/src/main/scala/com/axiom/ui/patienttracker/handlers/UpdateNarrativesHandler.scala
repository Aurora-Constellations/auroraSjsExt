package com.axiom.ui.patienttracker.handlers

import scala.scalajs.js
import com.axiom.ModelFetch
import com.axiom.messaging.*
import scala.concurrent.ExecutionContext.Implicits.global
import _root_.com.axiom.ui.patienttracker.EventChannels

object UpdateNarrativesHandler {
  def init(): Unit = {
    EventChannels.incoming.subscribe {
      case Request("updateNarratives", data: UpdateNarratives) =>
        println(s"[Webview] Update received: $data")
      case any =>
        println(s"[Webview] Unhandled request: $any")
        // update UI...
    }
  }
}

