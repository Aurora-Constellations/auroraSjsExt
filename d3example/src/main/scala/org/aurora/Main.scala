package org.aurora

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import typings.d3.mod as d3
import typings.d3Hierarchy.mod.HierarchyNode
import org.aurora.sjsast.*
import typings.elkjs.mod as elk
import typings.elkjs.libElkApiMod.{ElkNode, ElkEdge}
import typings.elkjs.mod.default as ELK
import scala.concurrent.ExecutionContext.Implicits.global
import typings.auroraLangium.distTypesSrcExtensionSrcParserParserMod.parseFromText
import scala.concurrent.Future

@JSImport("@find/**/HelloWorld.less", JSImport.Namespace)
@js.native
private object Stylesheet extends js.Object

val _ = Stylesheet // force initialization to prevent DCE (Dead Code Elimination) from removing the stylesheet

@main
def main(): Unit = {

  val fileUrl = "/TB00019445.aurora"
  println(s"Fetching file from $fileUrl...")

  dom
    .fetch(fileUrl)
    .toFuture
    .flatMap { response =>
      if (!response.ok) {
        throw new Exception(s"Failed to load file. Check if it is in the public folder. Status: ${response.statusText}")
      }
      response.text().toFuture
    }
    .flatMap { textContent =>
      println("File loaded successfully! Parsing string...")
      BrowserParser.parseString(textContent).toFuture
    }
    .flatMap { parsedAst =>
      // 1. Create the PCM
      val pcm = PCM(parsedAst)
      
      // 2. Set up ELK Layout Options dynamically
      val layoutOptions = LHMap(
        "elk.algorithm" -> "layered",
        "elk.direction" -> "TOP",
        "elk.spacing.nodeNode" -> "50", // Horizontal space between boxes
        "elk.layered.spacing.nodeNodeBetweenLayers" -> "60" // Vertical space between layers
      )
      
      println("Running ELK Layout...")
      // 3. Call ELK and extract the layout Future
      val elkGraph = AuroraElk.Graph(pcm, layoutOptions)
      elkGraph.graph 
    }
    .map { elkNode =>
      try {
        // 4. Transform the ELK Node into our typed D3 Node
        val d3Tree = AstTransformer.fromElkToD3Node(elkNode)

        println("Rendering ELK D3 tree...")
        drawElkLayout(d3Tree.toJS)

      } catch {
        case e: Exception =>
          println(s"Error transforming AST: ${e.getMessage}")
          e.printStackTrace()
      }
    }
    .recover { case e: Exception =>
      println(s"Pipeline error: ${e.getMessage}")
      e.printStackTrace()
    }
}

def drawElkLayout(data: js.Any): Unit = {
  val container = d3.select("body").asInstanceOf[js.Dynamic]
  container.selectAll("*").remove()

  val width = dom.window.innerWidth.toDouble
  val height = dom.window.innerHeight.toDouble

  val svg = container.append("svg")
    .attr("width", "100%")
    .attr("height", "100vh")
    .attr("viewBox", s"0 0 $width $height")
    .style("background-color", "#1e1e1e")
    .style("font-family", "sans-serif")
    .asInstanceOf[js.Dynamic]

  val g = svg.append("g").asInstanceOf[js.Dynamic]

  // Add Zoom and Pan capability
  val zoom = d3.zoom().asInstanceOf[js.Dynamic].on("zoom", (e: dom.Event) => {
    g.attr("transform", e.asInstanceOf[js.Dynamic].transform)
  })
  svg.call(zoom)

  // 1. Load data into D3 hierarchy to easily loop over it
  val root = d3.hierarchy[js.Any](data).asInstanceOf[js.Dynamic]
  
  // 2. Calculate the absolute X and Y screen coordinates 
  root.each((d: js.Dynamic) => {
    if (!js.isUndefined(d.parent) && d.parent != null) {
      d.absoluteX = d.parent.absoluteX.asInstanceOf[Double] + d.data.x.asInstanceOf[Double]
      d.absoluteY = d.parent.absoluteY.asInstanceOf[Double] + d.data.y.asInstanceOf[Double]
    } else {
      d.absoluteX = d.data.x.asInstanceOf[Double]
      d.absoluteY = d.data.y.asInstanceOf[Double]
    }
  })

  val nodes = root.descendants().asInstanceOf[js.Array[js.Dynamic]]
  val links = root.links().asInstanceOf[js.Array[js.Dynamic]]

  val colorPalette = js.Array("#d19a66", "#e06c75", "#98c379", "#61afef", "#c678dd", "#56b6c2")
  val getColor = (nodeType: String) => {
    val index = Math.abs(nodeType.hashCode) % colorPalette.length
    colorPalette(index)
  }

  // 3. Draw curved Links between the calculated node positions
  // 3. Extract Explicit ELK Edges and calculate their absolute screen positions
  val allEdges = js.Array[js.Dynamic]()
  root.each((d: js.Dynamic) => {
    if (!js.isUndefined(d.data.edges) && d.data.edges != null) {
      val nodeEdges = d.data.edges.asInstanceOf[js.Array[js.Dynamic]]
      nodeEdges.foreach { e => 
        e.parentAbsX = d.absoluteX
        e.parentAbsY = d.absoluteY
        allEdges.push(e)
      }
    }
  })

  // Debugging line to check the browser console
  println(s"Extracted ${allEdges.length} explicitly routed edges from ELK.")

  val lineGenerator = d3.line().asInstanceOf[js.Dynamic]
    .x((p: js.Dynamic) => p.x.asInstanceOf[Double])
    .y((p: js.Dynamic) => p.y.asInstanceOf[Double])

  // Draw the precise paths calculated by ELK
  g.selectAll(".link")
    .data(allEdges)
    .enter()
    .append("path")
    .attr("class", "link")
    .style("fill", "none")
    .style("stroke", "#555")
    .style("stroke-width", "2px")
    .attr("d", (d: js.Dynamic) => {
       // Explicitly cast to js.Array before asking for length or index
       if (!js.isUndefined(d.sections) && d.sections.asInstanceOf[js.Array[js.Dynamic]].length > 0) {
          val sectionsArray = d.sections.asInstanceOf[js.Array[js.Dynamic]]
          val section = sectionsArray(0)
          val parentX = d.parentAbsX.asInstanceOf[Double]
          val parentY = d.parentAbsY.asInstanceOf[Double]
          
          val points = js.Array[js.Dynamic]()
          
          points.push(js.Dictionary(
            "x" -> (section.startPoint.x.asInstanceOf[Double] + parentX), 
            "y" -> (section.startPoint.y.asInstanceOf[Double] + parentY)
          ).asInstanceOf[js.Dynamic])
          
          if (!js.isUndefined(section.bendPoints)) {
             section.bendPoints.asInstanceOf[js.Array[js.Dynamic]].foreach { bp => 
               points.push(js.Dictionary(
                 "x" -> (bp.x.asInstanceOf[Double] + parentX), 
                 "y" -> (bp.y.asInstanceOf[Double] + parentY)
               ).asInstanceOf[js.Dynamic])
             }
          }
          
          points.push(js.Dictionary(
            "x" -> (section.endPoint.x.asInstanceOf[Double] + parentX), 
            "y" -> (section.endPoint.y.asInstanceOf[Double] + parentY)
          ).asInstanceOf[js.Dynamic])
          
          val pathString = lineGenerator(points).asInstanceOf[String]
          if (pathString != null) pathString else ""
       } else {
          "" 
       }
    })

  // 4. Draw Nodes exactly where ELK specified
  val nodeSelection = g.selectAll(".node")
    .data(nodes)
    .enter()
    .append("g")
    .attr("transform", (d: js.Dynamic) => s"translate(${d.absoluteX}, ${d.absoluteY})")

  // The Rectangle
  nodeSelection.append("rect")
    .attr("width", (d: js.Dynamic) => d.data.width) // Strictly trust ELK's width!
    .attr("height", (d: js.Dynamic) => d.data.height)
    .attr("rx", 6)
    .attr("ry", 6)
    .style("fill", (d: js.Dynamic) => {
      val nType = if (!js.isUndefined(d.data.nodeType)) d.data.nodeType.asInstanceOf[String] else "Unknown"
      getColor(nType)
    })
    .style("stroke", "#333")
    .style("stroke-width", "2px")

  // The Text
  nodeSelection.append("text")
    .attr("x", (d: js.Dynamic) => d.data.width.asInstanceOf[Double] / 2) // Center text horizontally using ELK's width
    .attr("y", (d: js.Dynamic) => d.data.height.asInstanceOf[Double] / 2) // Center text vertically
    .attr("dy", "4px") 
    .attr("text-anchor", "middle")
    .style("fill", "#1e1e1e")
    .style("font-size", "12px")
    .style("font-weight", "bold")
    .text((d: js.Dynamic) => {
      val nodeName = d.data.name
      if (js.isUndefined(nodeName) || nodeName == null) "Unknown" else nodeName.toString
    })
}