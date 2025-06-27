package com.axiom.messaging

import scala.scalajs.js

trait MessageHandler[T <: js.Object] {
  def handle(msg: T): Unit
}