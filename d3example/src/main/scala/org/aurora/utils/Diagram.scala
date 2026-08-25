package org.aurora.utils

import org.aurora.visual.d3.D3Renderer
import org.aurora.sjsast.LHMap
import org.scalajs.dom
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import org.aurora.BrowserParser
import org.aurora.visual.elk.AuroraElk
import org.aurora.sjsast.PCM
import org.aurora.visual.d3.AstTransformer
import org.scalajs.dom.Element


object Diagram {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  def renderDiagram(textContent: String, renderer: D3Renderer, layoutOptions: LHMap[String, String]): Unit = {
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
}
