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

@JSImport("@find/**/HelloWorld.less", JSImport.Namespace)
@js.native
private object Stylesheet extends js.Object
val _ = Stylesheet

@main
def main(): Unit = {
  println("Aurora D3 Visualization ready. Listening for updates...")
  
  // Attach specifically to the container created in the HTML template
  val renderer = new D3Renderer("#d3-container")

  val layoutOptions = LHMap(
    "elk.algorithm" -> "layered",
    "elk.direction" -> "TOP",
    "elk.spacing.nodeNode" -> "10",
    "elk.layered.spacing.nodeNodeBetweenLayers" -> "30"
  )

  // Listen for the file content pushed from VS Code
  // dom.window.addEventListener("message", (event: dom.MessageEvent) => {
  //   val message = event.data.asInstanceOf[js.Dynamic]
  //   if (message.command.asInstanceOf[String] == "updateDiagram") {
  //     val textContent = message.data.asInstanceOf[String]
  //     println("Message received to D3")
  //     renderDiagram(textContent, renderer, layoutOptions)
  //   }
  // })
}

// Extracted for clean high-level reading
