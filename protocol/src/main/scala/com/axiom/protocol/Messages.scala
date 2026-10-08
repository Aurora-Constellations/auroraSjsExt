package com.axiom.protocol

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import scala.util.Try

object Protocol {

  /** Bumped whenever a message changes incompatibly. The webview announces it in [[ToExtension.Ready]] so a stale
    * `media/d3_main.js` next to a newer `extension.js` (or vice versa) is reported instead of silently doing nothing.
    */
  val Version: Int = 1
}

/** Messages the extension host sends to the D3 webview. */
sealed trait ToWebview

object ToWebview {

  /** Replace the drawn diagram. */
  @named("updateDiagram")
  final case class UpdateDiagram(data: DiagramNode) extends ToWebview

  /** The editor cursor moved; highlight the node whose source contains this (zero-based) position. */
  @named("highlightPosition")
  final case class HighlightPosition(line: Int, character: Int) extends ToWebview
}

/** Messages the D3 webview sends to the extension host. */
sealed trait ToExtension

object ToExtension {

  /** Sent once the webview is listening; the host answers by resending the latest diagram. */
  @named("ready")
  final case class Ready(protocolVersion: Int) extends ToExtension

  /** A node was clicked; the host selects `range` in the editor. */
  @named("nodeClicked")
  final case class NodeClicked(id: String, range: SourceRange) extends ToExtension
}

/** JSON encoding of the protocol. Platform-neutral on purpose: the VS Code specific hand-off to `postMessage` lives
  * in the bridges on each side. Decoding never throws; malformed or unknown messages are a `Left` with the reason.
  *
  * Sealed hierarchies are tagged by a `command` field (`{"command":"nodeClicked", ...}`), matching the messages the
  * extension and the webview exchanged before there was a protocol module. `None`, empty collections and defaulted
  * fields are omitted on write and optional on read.
  */
object Wire {
  // The macro needs the config as a literal expression, so it is spelled out for each root type.
  // Recursive types are allowed because the diagram is a tree; the input is trusted (our own extension and webview),
  // so jsoniter's stack-depth guard against hostile input is not needed. The `command` tag is accepted anywhere in the
  // object, so correctness does not depend on key order surviving the postMessage hop.
  private given JsonValueCodec[ToWebview] =
    JsonCodecMaker.make(CodecMakerConfig.withDiscriminatorFieldName(Some("command")).withRequireDiscriminatorFirst(false).withAllowRecursiveTypes(true))
  private given JsonValueCodec[ToExtension] =
    JsonCodecMaker.make(CodecMakerConfig.withDiscriminatorFieldName(Some("command")).withRequireDiscriminatorFirst(false).withAllowRecursiveTypes(true))

  def encode(msg: ToWebview): String = writeToString(msg)
  def encode(msg: ToExtension): String = writeToString(msg)

  def decodeToWebview(json: String): Either[String, ToWebview] = decode[ToWebview](json)
  def decodeToExtension(json: String): Either[String, ToExtension] = decode[ToExtension](json)

  private def decode[T: JsonValueCodec](json: String): Either[String, T] =
    Try(readFromString[T](json)).toEither.left.map(e => firstLine(String.valueOf(e.getMessage)))

  // jsoniter appends a hex dump of the input to its error messages; keep just the reason
  private def firstLine(message: String): String = message.takeWhile(_ != '\n').stripSuffix(", buf:")
}
