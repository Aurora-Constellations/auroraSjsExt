package com.axiom.ui.patienttracker.utils

import org.scalajs.dom.KeyboardEvent
import scala.scalajs.js
import scala.scalajs.js.timers.SetIntervalHandle

class KeyboardNavigation(
  moveAndScroll: Int => Unit
) {
  private var keyPressInterval: Option[SetIntervalHandle] = None
  private var activeKey: Option[Int] = None

  def keyboardHandler(e: KeyboardEvent): Unit = {
    e.keyCode match {
      case 40 => startKeyPressHandler(e.keyCode, () => moveAndScroll(1))
      case 38 => startKeyPressHandler(e.keyCode, () => moveAndScroll(-1))
      case _  => ()
    }
  }
  // Start handling long key press
  def startKeyPressHandler(keyCode: Int, action: () => Unit): Unit = {
    // If the key is already active, do nothing
    if (activeKey.contains(keyCode)) return
    // Mark the key as active
    activeKey = Some(keyCode)
    // Execute the action immediately
    action()
    // Clear any existing interval
    keyPressInterval.foreach(js.timers.clearInterval)
    // Start a new interval for continuous execution
    keyPressInterval = Some(js.timers.setInterval(100)(action()))
  }
  // Stop handling long key press
  def stopKeyPressHandler(e: KeyboardEvent): Unit = {
    // Only stop if the released key matches the active key
    if (activeKey.contains(e.keyCode)) {
      keyPressInterval.foreach(js.timers.clearInterval)
      keyPressInterval = None
      activeKey = None
    }
  }
}
