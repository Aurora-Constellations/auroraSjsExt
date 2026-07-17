package org.aurora

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import org.aurora.sjsast.*
import scala.concurrent.ExecutionContext.Implicits.global

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
    "elk.spacing.nodeNode" -> "50",
    "elk.layered.spacing.nodeNodeBetweenLayers" -> "60"
  )

  dom.fetch(fileUrl).toFuture
    .flatMap { response =>
      if (!response.ok) throw new Exception(s"Failed to load file. Status: ${response.statusText}")
      response.text().toFuture
    }
    .flatMap { textContent =>
      println("File loaded successfully! Parsing string...")
      BrowserParser.parseString(textContent).toFuture
    }
    .flatMap { parsedAst =>
      println("Running ELK Layout...")
      AuroraElk.Graph(PCM(parsedAst), layoutOptions).graph 
    }
    .map { elkNode =>
      println("Transforming and Rendering D3 tree...")
      val d3Tree = AstTransformer.fromElkToD3Node(elkNode)
      renderer.render(d3Tree.toJS)
    }
    .recover { case e: Exception =>
      println(s"Pipeline error: ${e.getMessage}")
      e.printStackTrace()
    }
}