package org.aurora

import org.scalajs.dom
import scala.scalajs.js

import org.aurora.visual.d3.D3Renderer
import org.aurora.sjsast.LHMap
import org.aurora.utils.Diagram

object D3MainObject {

  def main(args: Array[String]): Unit = {

    val renderer = new D3Renderer("#d3-container")

    val layoutOptions = LHMap(
      "elk.algorithm" -> "layered",
      "elk.direction" -> "TOP",
      "elk.spacing.nodeNode" -> "10",
      "elk.layered.spacing.nodeNodeBetweenLayers" -> "30"
    )

    dom.window.addEventListener(
  "message",
  (event: dom.MessageEvent) => {

    val message =
      event.data.asInstanceOf[js.Dynamic]

    if (message.command.asInstanceOf[String] == "updateDiagram") {

      val d3Tree = message.data

      renderer.render(d3Tree)
    }
  }
)

  }
}