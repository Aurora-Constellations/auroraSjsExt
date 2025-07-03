package com.axiom.ui.patienttracker

import scala.scalajs.js
import scala.scalajs.js.Dynamic
import com.axiom.messaging.*
import com.axiom.ui.patienttracker.handlers.*

object CommandDispatcher {
  def dispatch(data: js.Dynamic): Unit = {
    if (js.isUndefined(data.command)) {
      println("Received message does not contain `command` field.")
      return
    }

    val command = data.command.asInstanceOf[String]
    command match {
      case MessagingCommands.UpdateNarratives =>
        val msg = data.asInstanceOf[UpdateNarrativesMsg]
        UpdateNarrativesHandler.handle(msg)

      case other =>
        println(s"Unknown command: $other")
    }
  }
}
