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
  dom.window.addEventListener("message", (event: dom.MessageEvent) => {
    val message = event.data.asInstanceOf[js.Dynamic]
    if (message.command.asInstanceOf[String] == "updateDiagram") {
      val textContent = message.data.asInstanceOf[String]
      println("Message received to D3")
      renderDiagram(textContent, renderer, layoutOptions)
    }
  })
}

// Extracted for clean high-level reading
private def renderDiagram(textContent: String, renderer: D3Renderer, layoutOptions: LHMap[String, String]): Unit = {
  // Clear the previous render safely
  val container = dom.document.getElementById("d3-container")
  if (container != null) container.innerHTML = ""

  val pipeline = for {
    _ <- Future { println("String received! Parsing...") }
    parsedAst <- BrowserParser.parseString(textContent)
    
    _ <- Future { println("Running ELK Layout...") }
    elkNode <- AuroraElk.Graph(PCM(parsedAst), layoutOptions).graph

  } yield {
    println("Transforming and Rendering D3 tree...")
    val d3Tree = AstTransformer.fromElkToD3Node(elkNode, PCM(parsedAst))
    renderer.render(d3Tree.toJS)
  }

  pipeline.recover { case e: Exception =>
    println(s"Pipeline error: ${e.getMessage}")
    if (container != null) {
      container.innerHTML = s"""<div style="color:red; padding:10px;">Error rendering diagram: ${e.getMessage}</div>"""
    }
    e.printStackTrace()
  }
}