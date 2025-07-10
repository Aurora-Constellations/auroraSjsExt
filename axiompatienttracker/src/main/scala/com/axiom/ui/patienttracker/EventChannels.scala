package com.axiom.ui.patienttracker

import com.axiom.messaging.*

object EventChannels {
  val incoming: EventBus[Message] = new EventBus[Message]
  val outgoing: EventBus[Message] = new EventBus[Message]
}
