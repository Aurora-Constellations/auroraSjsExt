package com.axiom.messaging

import scala.collection.mutable

class EventBus[T] {
  private val subscribers = mutable.ListBuffer[T => Unit]()

  def subscribe(callback: T => Unit): Unit =
    subscribers += callback

  def publish(event: T): Unit =
    subscribers.foreach(_(event))
}
