package com.axiom.messaging

import scala.scalajs.js

@js.native
trait CreateAuroraFileMsg extends js.Object {
  val command: String
  val fileName: String
}

@js.native
trait UpdateNarrativesMsg extends js.Object {
  val command: String
  val source: String
  val unitNumber: String
  val flag: String
}

@js.native
trait UpdatedNarrativesMsg extends js.Object {
  val command: String
  val message: String
}

@js.native
trait OpenAuroraFileMsg extends js.Object {
  val command: String
  val fileName: String
}

@js.native
trait AddedToDBMsg extends js.Object {
  val command: String
  val fileName: String
}