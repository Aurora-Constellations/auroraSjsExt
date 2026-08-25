package org.aurora

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import org.aurora.sjsast.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.aurora.visual.elk.AuroraElk
import org.aurora.visual.d3.AstTransformer
import org.aurora.visual.d3.D3Renderer
import org.aurora.utils.Diagram

@JSImport("@find/**/HelloWorld.less", JSImport.Namespace)
@js.native
private object Stylesheet extends js.Object
val _ = Stylesheet

object D3MainObject {

  def main(args: Array[String]): Unit = {
  println("D3 MAIN STARTED")
  
  // Attach specifically to the container created in the HTML template
  val renderer = new D3Renderer("#d3-container")

  val layoutOptions = LHMap(
    "elk.algorithm" -> "layered",
    "elk.direction" -> "TOP",
    "elk.spacing.nodeNode" -> "10",
    "elk.layered.spacing.nodeNodeBetweenLayers" -> "30"
  )

  // Listen for the file content pushed from VS Code
  dom.window.addEventListener("message", (event: dom.MessageEvent) => {
    println("Message received by D3")
    val message = event.data.asInstanceOf[js.Dynamic]
    if (message.command.asInstanceOf[String] == "updateDiagram") {
      val textContent = message.data.asInstanceOf[String]
      Diagram.renderDiagram(textContent, renderer, layoutOptions)
    }
  })
}

// Extracted for clean high-level reading


}


