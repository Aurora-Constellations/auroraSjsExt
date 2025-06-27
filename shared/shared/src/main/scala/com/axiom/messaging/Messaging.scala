package com.axiom.messaging

import scala.scalajs.js
import scala.scalajs.js.Dynamic.literal

sealed trait Message

case class Request[T](
    command: String, // e.g., "updateNarratives", "createAuroraFile", "openAuroraFile"
    data: T
) extends Message

case class Response[R](
    command: String, // e.g., "updatedNarratives", "addedToDB"
    result: R
) extends Message

trait ToJsObject {
  def toJsObject(command: String): js.Dynamic
}

case class UpdateNarratives(
    source: String, // e.g., "vscode-extension"
    unitNumber: String,
    flag: String // e.g., "12", "1", "2", "0"
) extends ToJsObject {
  def toJsObject(command: String): js.Dynamic = {
    literal(
      command = command,
      source = source,
      unitNumber = unitNumber,
      flag = flag
    )
  }
}

case class UpdatedNarratives(
    message: String
) extends ToJsObject {
  def toJsObject(command: String): js.Dynamic = {
    literal(
      command = command,
      message = message
    )
  }
}

case class CreateAuroraFile(
    fileName: String
) extends ToJsObject {
  def toJsObject(command: String): js.Dynamic = {
    literal(
      command = command,
      fileName = fileName
    )
  }
}

case class OpenAuroraFile(
    fileName: String
) extends ToJsObject {
  def toJsObject(command: String): js.Dynamic = {
    literal(
      command = command,
      fileName = fileName
    )
  }
}

case class AddedToDB(
    fileName: String
) extends ToJsObject {
  def toJsObject(command: String): js.Dynamic = {
    literal(
      command = command,
      fileName = fileName
    )
  }
}
