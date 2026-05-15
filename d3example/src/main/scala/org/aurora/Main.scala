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

  // 1. The relative path to your file in the public folder
  val fileUrl = "/TB00019445.aurora"

  println(s"Fetching file from $fileUrl...")

  // 2. Fetch the file asynchronously
  dom
    .fetch(fileUrl)
    .toFuture
    .flatMap { response =>
      if (!response.ok) {
        throw new Exception(s"Failed to load file. Check if it is in the public folder. Status: ${response.statusText}")
      }
      // Extract the text content from the response
      response.text().toFuture
    }
    .flatMap { textContent =>
      println("File loaded successfully! Parsing string...")

      // 3. Pass the fetched string to your parser
      BrowserParser.parseString(textContent).toFuture
    }
    .map { parsedAst =>
      try {
        // 4. Transform and render
        val pcm = PCM(parsedAst)
        val d3Tree = AstTransformer.toD3Node(pcm)

        println("Rendering D3 tree...")
        drawNetwork(d3Tree.toJS)

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

def drawTree(data: js.Any): Unit = {
  // 1. Select the #app div and clear it so we don't duplicate on hot-reload
  val container = d3.select("body")
  container.selectAll("*").remove()

  // 2. Append a brand new SVG to the container
  val svg = container
    .append("svg")
    .attr("width", "100%")
    .attr("height", "100vh") // Use 100vh to fill the height of the window
    .attr("viewBox", "0 0 1000 600")
    .style("background-color", "#f8f9fa") // Light gray background
    .style("font-family", "sans-serif")

  val width = 1000.0
  val height = 600.0

  val g = svg.append("g").attr("transform", "translate(80, 40)")

  val treeLayout = d3.tree[js.Any]().size(js.Tuple2(height - 80, width - 260))

  val root = d3.hierarchy[js.Any](data)
  treeLayout(root)

  val descendants = root.descendants()
  val links = descendants.slice(1, descendants.length.toInt)

  // 3. Native Scala Color Scale (Bypasses D3 typing errors completely)
  val colorPalette = js.Array(
    "#1f77b4",
    "#ff7f0e",
    "#2ca02c",
    "#d62728",
    "#9467bd",
    "#8c564b",
    "#e377c2",
    "#7f7f7f",
    "#bcbd22",
    "#17becf"
  )
  val getColor = (nodeType: String) => {
    val index = Math.abs(nodeType.hashCode) % colorPalette.length
    colorPalette(index)
  }

  // 4. Draw Links with smooth, elegant curves
  g.selectAll(".link")
    .data(links)
    .enter()
    .append("path")
    .attr("class", "link")
    .style("fill", "none")
    .style("stroke", "#b8c2cc")
    .style("stroke-width", "2px")
    .style("opacity", "0.7")
    .asInstanceOf[js.Dynamic]
    .attr(
      "d",
      (d: js.Dynamic) => {
        val parent = d.parent
        s"M${d.y},${d.x}C${(d.y.asInstanceOf[Double] + parent.y.asInstanceOf[Double]) / 2},${d.x} ${(d.y
            .asInstanceOf[Double] + parent.y.asInstanceOf[Double]) / 2},${parent.x} ${parent.y},${parent.x}"
      }
    )

  // 5. Draw Nodes
  val nodeSelection = g
    .selectAll(".node")
    .data(descendants)
    .enter()
    .append("g")
    .attr("class", "node")
    .style("cursor", "pointer")
    .asInstanceOf[js.Dynamic]
    .attr("transform", (d: js.Dynamic) => s"translate(${d.y},${d.x})")

  // Add circles with colors based on nodeType
  nodeSelection
    .append("circle")
    .attr("r", 8) // Larger circles
    .style(
      "fill",
      (d: js.Dynamic) => {
        val nType = if (d.data.nodeType != null) d.data.nodeType.asInstanceOf[String] else "Unknown"
        getColor(nType)
      }
    )
    .style("stroke", "#fff")
    .style("stroke-width", "2px")
    // Hover effects via inline events
    .on(
      "mouseover",
      (e: dom.Event, d: js.Dynamic) => {
        d3.select(e.currentTarget.asInstanceOf[dom.Element])
          .asInstanceOf[js.Dynamic] // Bypass strict Selection types to use transition
          .transition()
          .duration(200)
          .attr("r", 12)
      }
    )
    .on(
      "mouseout",
      (e: dom.Event, d: js.Dynamic) => {
        d3.select(e.currentTarget.asInstanceOf[dom.Element])
          .asInstanceOf[js.Dynamic]
          .transition()
          .duration(200)
          .attr("r", 8)
      }
    )

  // 6. Add Text with a "halo" effect to ensure it is readable over lines
  nodeSelection
    .append("text")
    .attr("dy", 4)
    .attr(
      "x",
      (d: js.Dynamic) => {
        val isLeaf = js.isUndefined(d.children) || d.children.asInstanceOf[js.Array[js.Any]].length == 0
        if (isLeaf) 12 else -12
      }
    )
    .style(
      "text-anchor",
      (d: js.Dynamic) => {
        val isLeaf = js.isUndefined(d.children) || d.children.asInstanceOf[js.Array[js.Any]].length == 0
        if (isLeaf) "start" else "end"
      }
    )
    .style("font-size", "14px")
    .style("font-weight", "bold")
    .style("fill", "#111827")
    .style("paint-order", "stroke") // Draws stroke behind the text fill
    .style("stroke", "#ffffff") // Matches background color
    .style("stroke-width", "4px") // Creates the halo
    .style("stroke-linejoin", "round")
    .text((d: js.Dynamic) => {
      val nodeName = d.data.name
      if (js.isUndefined(nodeName) || nodeName == null) "Unnamed Node"
      else nodeName.toString
    })
}


def drawNetwork(data: js.Any): Unit = {
  // 1. Select and clear container (cast to dynamic to bypass selection typings)
  val container = d3.select("body").asInstanceOf[js.Dynamic]
  container.selectAll("*").remove()

  // 2. Setup SVG with a dark theme
  val width = dom.window.innerWidth.toDouble
  val height = dom.window.innerHeight.toDouble

  val svg = container.append("svg")
    .attr("width", "100%")
    .attr("height", "100vh")
    .attr("viewBox", s"0 0 $width $height")
    .style("background-color", "#1e1e1e") 
    .style("font-family", "sans-serif")
    .asInstanceOf[js.Dynamic]

  // Add arrow markers for directed edges
  svg.append("defs").append("marker")
    .attr("id", "arrow")
    .attr("viewBox", "0 -5 10 10")
    .attr("refX", 18)
    .attr("refY", 0)
    .attr("markerWidth", 6)
    .attr("markerHeight", 6)
    .attr("orient", "auto")
    .append("path")
    .attr("fill", "#888")
    .attr("d", "M0,-5L10,0L0,5")

  val g = svg.append("g").asInstanceOf[js.Dynamic]

  // 3. Process Data into Nodes and Links
  val root = d3.hierarchy[js.Any](data).asInstanceOf[js.Dynamic]
  val rawNodes = root.descendants().asInstanceOf[js.Array[js.Dynamic]]
  val rawLinks = root.links().asInstanceOf[js.Array[js.Dynamic]]

  // 3a. Filter out nodes where nodeType == "Unknown"
  val nodes = rawNodes.filter { n =>
    val nType = if (js.isUndefined(n.data.nodeType)) "" else n.data.nodeType.asInstanceOf[String]
    nType != "Unknown"
  }

  // 3b. Filter out any links that connect to an "Unknown" node
  val allLinks = rawLinks.filter { l =>
    val srcType = if (js.isUndefined(l.source.data.nodeType)) "" else l.source.data.nodeType.asInstanceOf[String]
    val tgtType = if (js.isUndefined(l.target.data.nodeType)) "" else l.target.data.nodeType.asInstanceOf[String]
    srcType != "Unknown" && tgtType != "Unknown"
  }
  // Cross-reference links
  nodes.foreach { n =>
    val nType = if (js.isUndefined(n.data.nodeType)) "" else n.data.nodeType.asInstanceOf[String]
    if (nType == "Reference") {
      val rawName = n.data.name.asInstanceOf[String]
      val targetName = rawName.replace("Ref: ", "").trim()

      val targetNode = nodes.find { t =>
        val tName = if (js.isUndefined(t.data.name)) "" else t.data.name.asInstanceOf[String]
        tName == targetName || tName == s"Coord: $targetName" || tName.endsWith(s": $targetName")
      }

      if (targetNode.isDefined) {
        val sourceNode = if (!js.isUndefined(n.parent) && n.parent != null && !js.isUndefined(n.parent.parent) && n.parent.parent != null) n.parent.parent else n.parent
        
        allLinks.push(js.Dynamic.literal(
          "source" -> sourceNode,
          "target" -> targetNode.get,
          "isCrossLink" -> true
        ))
      }
    }
  }

  // 4. Color Palette
  val colorPalette = js.Array("#d19a66", "#e06c75", "#98c379", "#61afef", "#c678dd", "#56b6c2")
  val getColor = (nodeType: String) => {
    val index = Math.abs(nodeType.hashCode) % colorPalette.length
    colorPalette(index)
  }

  // 5. Initialize Force Simulation (Cast to dynamic to bypass strict force typings)
  val simulation = d3.forceSimulation().asInstanceOf[js.Dynamic]
  
  // Assign nodes dynamically
  simulation.nodes(nodes)

  // Build the link force
  val linkForce = d3.forceLink().asInstanceOf[js.Dynamic]
  linkForce.links(allLinks)
  linkForce.distance(120)
  linkForce.strength(1)

  // Build the charge force (repels nodes apart)
  val chargeForce = d3.forceManyBody().asInstanceOf[js.Dynamic]
  chargeForce.strength(-400)

  // Build the center force
  val centerForce = d3.forceCenter(width / 2, height / 2).asInstanceOf[js.Dynamic]

  // Build the collision force (prevents overlap)
  val collideForce = d3.forceCollide().asInstanceOf[js.Dynamic]
  collideForce.radius(40)

  // Attach all forces to the simulation
  simulation.force("link", linkForce)
  simulation.force("charge", chargeForce)
  simulation.force("center", centerForce)
  simulation.force("collide", collideForce)

  // 6. Draw Links
  val linkSelection = g.selectAll(".link")
    .data(allLinks)
    .enter().append("line")
    .attr("class", "link")
    .asInstanceOf[js.Dynamic] // Critical cast for .style handling

  linkSelection
    .style("stroke", (d: js.Dynamic) => if (!js.isUndefined(d.isCrossLink) && d.isCrossLink.asInstanceOf[Boolean]) "#e06c75" else "#555")
    .style("stroke-width", (d: js.Dynamic) => if (!js.isUndefined(d.isCrossLink) && d.isCrossLink.asInstanceOf[Boolean]) "2px" else "1px")
    .style("stroke-dasharray", (d: js.Dynamic) => if (!js.isUndefined(d.isCrossLink) && d.isCrossLink.asInstanceOf[Boolean]) "5,5" else "none")
    .attr("marker-end", "url(#arrow)")

  // 7. Draw Nodes
  val nodeSelection = g.selectAll(".node")
    .data(nodes)
    .enter().append("g")
    .attr("class", "node")
    .style("cursor", "grab")
    .asInstanceOf[js.Dynamic]

  // Drag Behaviors (explicit Double cast for event.active)
  val dragStart = (e: dom.Event, d: js.Dynamic) => {
    val event = e.asInstanceOf[js.Dynamic]
    if (event.active.asInstanceOf[Double] == 0.0) simulation.alphaTarget(0.3).restart()
    d.fx = d.x
    d.fy = d.y
  }
  val dragMove = (e: dom.Event, d: js.Dynamic) => {
    val event = e.asInstanceOf[js.Dynamic]
    d.fx = event.x
    d.fy = event.y
  }
  val dragEnd = (e: dom.Event, d: js.Dynamic) => {
    val event = e.asInstanceOf[js.Dynamic]
    if (event.active.asInstanceOf[Double] == 0.0) simulation.alphaTarget(0)
    d.fx = null
    d.fy = null
  }

  nodeSelection.call(
    d3.drag().asInstanceOf[js.Dynamic]
      .on("start", dragStart)
      .on("drag", dragMove)
      .on("end", dragEnd)
  )

  // Append pill shapes
  nodeSelection.append("rect")
    .attr("rx", 12)
    .attr("ry", 12)
    .attr("x", -10)
    .attr("y", -14)
    .attr("height", 28)
    .style("fill", (d: js.Dynamic) => {
      val nType = if (!js.isUndefined(d.data.nodeType)) d.data.nodeType.asInstanceOf[String] else "Unknown"
      getColor(nType)
    })
    .style("stroke", "#333")
    .style("stroke-width", "1.5px")

  // Append Text
  nodeSelection.append("text")
    .attr("dy", 4)
    .attr("text-anchor", "middle")
    .style("font-size", "12px")
    .style("font-weight", "bold")
    .style("fill", "#1e1e1e")
    .text((d: js.Dynamic) => {
      val nodeName = d.data.name
      if (js.isUndefined(nodeName) || nodeName == null) "Unnamed" else nodeName.toString
    })

  // Dynamic width adjustment
  nodeSelection.selectAll("rect")
    .attr("width", (d: js.Dynamic) => {
      val textLength = if (!js.isUndefined(d.data.name)) d.data.name.toString.length else 7
      (textLength * 7) + 20
    })
    .attr("x", (d: js.Dynamic) => {
      val textLength = if (!js.isUndefined(d.data.name)) d.data.name.toString.length else 7
      -((textLength * 7) + 20) / 2
    })

  // 8. Tick function
  simulation.on("tick", () => {
    linkSelection
      .attr("x1", (d: js.Dynamic) => d.source.x)
      .attr("y1", (d: js.Dynamic) => d.source.y)
      .attr("x2", (d: js.Dynamic) => d.target.x)
      .attr("y2", (d: js.Dynamic) => d.target.y)

    nodeSelection.attr("transform", (d: js.Dynamic) => s"translate(${d.x},${d.y})")
  })

  // Zoom and Pan capability
  val zoom = d3.zoom().asInstanceOf[js.Dynamic].on("zoom", (e: dom.Event) => {
    g.attr("transform", e.asInstanceOf[js.Dynamic].transform)
  })
  svg.call(zoom)
}