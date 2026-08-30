package org.aurora

import org.scalajs.dom
import scala.scalajs.js

import org.aurora.visual.d3.D3Renderer
import org.aurora.sjsast.LHMap
import org.aurora.utils.Diagram

object D3MainObject {

  def main(args: Array[String]): Unit = {

    dom.document.body.insertAdjacentHTML(
      "beforeend",
      "<h2>BEFORE D3 RENDERER</h2>"
    )

    val renderer = new D3Renderer("#d3-container")

    dom.document.body.insertAdjacentHTML(
      "beforeend",
      "<h2>AFTER D3 RENDERER</h2>"
    )

    val layoutOptions = LHMap(
      "elk.algorithm" -> "layered",
      "elk.direction" -> "TOP",
      "elk.spacing.nodeNode" -> "10",
      "elk.layered.spacing.nodeNodeBetweenLayers" -> "30"
    )

    dom.document.body.insertAdjacentHTML(
      "beforeend",
      "<h2>AFTER LHMAP</h2>"
    )

    dom.window.addEventListener(
  "message",
  (event: dom.MessageEvent) => {

    dom.document.body.insertAdjacentHTML(
      "beforeend",
      "<h2>MESSAGE RECEIVED</h2>"
    )

    val message =
      event.data.asInstanceOf[js.Dynamic]

    dom.document.body.insertAdjacentHTML(
      "beforeend",
      "<h2>MESSAGE CAST OK</h2>"
    )

    if (message.command.asInstanceOf[String] == "updateDiagram") {

      val d3Tree = message.data

      dom.document.body.insertAdjacentHTML(
        "beforeend",
        "<h2 style='color:green'>D3 TREE RECEIVED</h2>"
      )

      renderer.render(d3Tree)
    }
  }
)

    dom.document.body.insertAdjacentHTML(
      "beforeend",
      "<h2>LISTENER INSTALLED</h2>"
    )
  }
}