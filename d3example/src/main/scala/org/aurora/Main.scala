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

val _ = Stylesheet // force initialization to prevent DCE

@main
def main(): Unit = {
  val fileUrl = "/TBSimple.aurora"
  println(s"Fetching file from $fileUrl...")

  val renderer = new D3Renderer("body")

  val layoutOptions = LHMap(
    "elk.algorithm" -> "layered",
    "elk.direction" -> "TOP",
    "elk.spacing.nodeNode" -> "10",
    "elk.layered.spacing.nodeNodeBetweenLayers" -> "30"
  )

  // Define the asynchronous pipeline using a for comprehension
  val pipeline = for {
    response <- dom.fetch(fileUrl).toFuture
    textContent <- {
      if (!response.ok) throw new Exception(s"Failed to load file. Status: ${response.statusText}")
      response.text().toFuture
    }
    
    _ <- Future { println("File loaded successfully! Parsing string...") }
    parsedAst <- BrowserParser.parseString(textContent)
    
    _ <- Future { println("Running ELK Layout...") }
    elkNode <- AuroraElk.Graph(PCM(parsedAst), layoutOptions).graph

  } yield {
    println("Transforming and Rendering D3 tree...")
    val d3Tree = AstTransformer.fromElkToD3Node(elkNode, PCM(parsedAst))
    renderer.render(d3Tree.toJS)
  }

  // 3. Attach your recovery logic to the completed pipeline
  pipeline.recover { case e: Exception =>
    println(s"Pipeline error: ${e.getMessage}")
    e.printStackTrace()
  }
}