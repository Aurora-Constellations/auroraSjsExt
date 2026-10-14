package org.aurora

import org.scalajs.dom
import scala.scalajs.js

import org.aurora.visual.d3.{D3AppRenderer, D3Renderer, FilterBanner, toSafeOption}
import org.aurora.sjsast.LHMap

object D3MainObject {

  def main(args: Array[String]): Unit =
    // The extension marks the page for the end-user layout (`sbt openUI`); otherwise keep the developer diagram
    if (dom.document.body.getAttribute("data-aurora-mode") == "app") appMain()
    else classicMain()

  private def appMain(): Unit = {

    val renderer = new D3AppRenderer("#d3-container")
    val banner = new FilterBanner(renderer.stageElement.parentNode.asInstanceOf[dom.Element], Some(renderer.stageElement))

    dom.window.addEventListener(
      "message",
      (event: dom.MessageEvent) => {

        val message =
          event.data.asInstanceOf[js.Dynamic]

        if (message.command.asInstanceOf[String] == "updateDiagram") {

          val title = message.title.asInstanceOf[js.UndefOr[String]].toSafeOption
          renderer.render(message.data, title)
        } else if (message.command.asInstanceOf[String] == "filterStatus") {

          banner.show(message.text.asInstanceOf[js.UndefOr[String]].toSafeOption)
        } else if (message.command.asInstanceOf[String] == "highlightPosition") {

          renderer.highlightAt(
            message.line.asInstanceOf[Int],
            message.character.asInstanceOf[Int]
          )
        }
      }
    )

    // The webview is rebuilt whenever its view is hidden and shown again; ask for the diagram it lost
    renderer.notifyReady()
  }

  private def classicMain(): Unit = {

    val renderer = new D3Renderer("#d3-container")
    val banner = new FilterBanner(dom.document.body)

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
    } else if (message.command.asInstanceOf[String] == "filterStatus") {

      banner.show(message.text.asInstanceOf[js.UndefOr[String]].toSafeOption)
    } else if (message.command.asInstanceOf[String] == "highlightPosition") {

      renderer.highlightAt(
        message.line.asInstanceOf[Int],
        message.character.asInstanceOf[Int]
      )
    }
  }
)

  }
}