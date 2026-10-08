package com.axiom.protocol

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class WireSpec extends AnyFunSuite with Matchers {

  private val range = SourceRange(1, 2, 3, 4)

  private val diagram = DiagramNode(
    name = "root",
    nodeType = "Coordinate",
    nodeQualifier = "Normal",
    x = 0,
    y = 0,
    width = 100,
    height = 40,
    id = "root%%Coordinate",
    range = Some(range),
    edges = List(
      DiagramEdge("a->b%%DraftEdge", List(EdgeSection(Point(0, 1), Point(2, 3), List(Point(1, 1), Point(1, 2)))))
    ),
    children = List(
      DiagramNode("child", "Reference", "Draft", 10, 20, 70, 40, id = "child%%Reference")
    )
  )

  test("ToWebview round-trips") {
    val messages = List[ToWebview](ToWebview.UpdateDiagram(diagram), ToWebview.HighlightPosition(7, 12))
    messages.foreach(m => Wire.decodeToWebview(Wire.encode(m)) shouldBe Right(m))
  }

  test("ToExtension round-trips") {
    val messages = List[ToExtension](ToExtension.Ready(Protocol.Version), ToExtension.NodeClicked("n1", range))
    messages.foreach(m => Wire.decodeToExtension(Wire.encode(m)) shouldBe Right(m))
  }

  // The wire shape is what DevTools shows and what older code produced: a flat object tagged by `command`.
  test("wire shape matches the pre-protocol messages") {
    Wire.encode(ToWebview.HighlightPosition(7, 12)) shouldBe
      """{"command":"highlightPosition","line":7,"character":12}"""

    Wire.encode(ToExtension.NodeClicked("n1", range)) shouldBe
      """{"command":"nodeClicked","id":"n1","range":{"startLine":1,"startChar":2,"endLine":3,"endChar":4}}"""

    Wire.encode(ToExtension.Ready(1)) shouldBe """{"command":"ready","protocolVersion":1}"""

    Wire.encode(ToWebview.UpdateDiagram(diagram)) should startWith("""{"command":"updateDiagram","data":{"name":"root"""")
  }

  test("optional and defaulted fields may be omitted") {
    val json = """{"command":"updateDiagram","data":{"name":"n","nodeType":"t","nodeQualifier":"q","x":1,"y":2,"width":3,"height":4}}"""
    Wire.decodeToWebview(json) shouldBe Right(
      ToWebview.UpdateDiagram(DiagramNode("n", "t", "q", 1, 2, 3, 4))
    )
  }

  test("the command tag may appear anywhere in the object") {
    Wire.decodeToWebview("""{"line":7,"character":12,"command":"highlightPosition"}""") shouldBe
      Right(ToWebview.HighlightPosition(7, 12))
  }

  test("a missing range is omitted on write and accepted as absent or null on read") {
    val node = DiagramNode("n", "t", "q", 1, 2, 3, 4)
    Wire.encode(ToWebview.UpdateDiagram(node)) should not include "range"

    val withNull = """{"command":"updateDiagram","data":{"name":"n","nodeType":"t","nodeQualifier":"q","x":1,"y":2,"width":3,"height":4,"range":null}}"""
    Wire.decodeToWebview(withNull) shouldBe Right(ToWebview.UpdateDiagram(node))
  }

  test("unknown commands and malformed payloads are Left, not exceptions") {
    Wire.decodeToWebview("""{"command":"nope"}""").isLeft shouldBe true
    Wire.decodeToWebview("""{"line":1}""").isLeft shouldBe true
    Wire.decodeToWebview("not json").isLeft shouldBe true
    // errors are a one-line reason, not a hex dump of the input
    Wire.decodeToWebview("""{"command":"nope"}""").left.exists(!_.contains("\n")) shouldBe true
    // a message for the other direction is rejected
    Wire.decodeToExtension(Wire.encode(ToWebview.HighlightPosition(1, 1))).isLeft shouldBe true
  }
}
