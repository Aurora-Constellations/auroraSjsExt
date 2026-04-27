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
import scala.concurrent.Future

@JSImport("@find/**/HelloWorld.less", JSImport.Namespace)
@js.native private object Stylesheet extends js.Object

val _ = Stylesheet // force initialization to prevent DCE (Dead Code Elimination) from removing the stylesheet

// @main def main(): Unit = {

//   //draws animated rotating globe and random lines around it 
//   d3canvassphere.start

//   //draws circles in svg
//   d3svgcircles.start()

//   //
//   d3svgpath.start()
//   d3svgaxis.start()
//   d3svgforcelink.start()
//   d3svgbarchart.start()



//   //when dom is loaded creates basic form
//   renderOnDomContentLoaded(
//     container = dom.document.querySelector("#app"),
//     rootNode = {
//       div(
//         cls("Main"),
//         h1("Laminar Template (scroll down to see d3 examples)"),
//         HelloWorld(),
//       )
//     }
//   )
// }

@main
  def main(): Unit = {
    // 1. The sample Aurora file content
    // val auroraCode = """
    //   |Qu name "Example Node" {
    //   |  Value 42
    //   |}
    // """.stripMargin

    val ref = QuReference(QU(LHSet('~')), "chf")
    val oc = OrderCoordinate("OC1", LHSet(NL_STATEMENT("test")), LHSet(QuReferences(LHSet(ref))))
    val ngo = NGO(name="NGO1", ordercoord=LHSet(oc), narratives = LHSet(), qurefs=LHSet(QuReferences(LHSet())), qu=LHSet())
    val orders = Orders(ngo=LHSet(ngo), narratives = LHSet())
    val pcm = PCM(LHMap("Orders" -> orders))

    
    // val graph = AstTransformer.toElkRoot(pcm) /* Create ELK graph object from PCM */
    // println(graph.children.toString())
    // println(graph.children)
    // println(graph.edges.map(e => e.asInstanceOf[ElkEdge].id_ElkEdge))

    // val elk = new ELK()
    // val layoutFuture = elk.layout(graph).toFuture
    

    // 2. Parse the string using your existing parser logic
    // Replace 'parse' with your actual parser entry point from pcmalgebra
    // val parsedAst = org.aurora.sjsast.package.parse(auroraCode)

    // 3. Transform AST to D3 format
    // val d3Data = AstTransformer.toD3(pcm)
    // println(d3Data) // Log the transformed data to verify it's correct

    // // 4. Render
    // drawTree(d3Data)
  }

def drawTree(data: js.Any): Unit = {
  // 1. Select the #app div and clear it so we don't duplicate on hot-reload
  val container = d3.select("body")
  container.selectAll("*").remove()

  // 2. Append a brand new SVG to the container
  val svg = container.append("svg")
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
  val colorPalette = js.Array("#1f77b4", "#ff7f0e", "#2ca02c", "#d62728", "#9467bd", "#8c564b", "#e377c2", "#7f7f7f", "#bcbd22", "#17becf")
  val getColor = (nodeType: String) => {
    val index = Math.abs(nodeType.hashCode) % colorPalette.length
    colorPalette(index)
  }

  // 4. Draw Links with smooth, elegant curves
  g.selectAll(".link")
    .data(links)
    .enter().append("path")
    .attr("class", "link")
    .style("fill", "none")
    .style("stroke", "#b8c2cc")
    .style("stroke-width", "2px")
    .style("opacity", "0.7")
    .asInstanceOf[js.Dynamic]
    .attr("d", (d: js.Dynamic) => {
      val parent = d.parent
      s"M${d.y},${d.x}C${(d.y.asInstanceOf[Double] + parent.y.asInstanceOf[Double]) / 2},${d.x} ${(d.y.asInstanceOf[Double] + parent.y.asInstanceOf[Double]) / 2},${parent.x} ${parent.y},${parent.x}"
    })

  // 5. Draw Nodes
  val nodeSelection = g.selectAll(".node")
    .data(descendants)
    .enter().append("g")
    .attr("class", "node")
    .style("cursor", "pointer")
    .asInstanceOf[js.Dynamic]
    .attr("transform", (d: js.Dynamic) => s"translate(${d.y},${d.x})")

  // Add circles with colors based on nodeType
  nodeSelection.append("circle")
    .attr("r", 8) // Larger circles
    .style("fill", (d: js.Dynamic) => {
      val nType = if (d.data.nodeType != null) d.data.nodeType.asInstanceOf[String] else "Unknown"
      getColor(nType)
    })
    .style("stroke", "#fff")
    .style("stroke-width", "2px")
    // Hover effects via inline events
    .on("mouseover", (e: dom.Event, d: js.Dynamic) => {
       d3.select(e.currentTarget.asInstanceOf[dom.Element])
         .asInstanceOf[js.Dynamic] // Bypass strict Selection types to use transition
         .transition().duration(200).attr("r", 12)
    })
    .on("mouseout", (e: dom.Event, d: js.Dynamic) => {
       d3.select(e.currentTarget.asInstanceOf[dom.Element])
         .asInstanceOf[js.Dynamic] 
         .transition().duration(200).attr("r", 8)
    })

  // 6. Add Text with a "halo" effect to ensure it is readable over lines
  nodeSelection.append("text")
    .attr("dy", 4)
    .attr("x", (d: js.Dynamic) => {
      val isLeaf = js.isUndefined(d.children) || d.children.asInstanceOf[js.Array[js.Any]].length == 0
      if (isLeaf) 12 else -12
    })
    .style("text-anchor", (d: js.Dynamic) => {
      val isLeaf = js.isUndefined(d.children) || d.children.asInstanceOf[js.Array[js.Any]].length == 0
      if (isLeaf) "start" else "end"
    })
    .style("font-size", "14px")
    .style("font-weight", "bold")
    .style("fill", "#111827")
    .style("paint-order", "stroke") // Draws stroke behind the text fill
    .style("stroke", "#ffffff")     // Matches background color
    .style("stroke-width", "4px")   // Creates the halo
    .style("stroke-linejoin", "round")
    .text((d: js.Dynamic) => {
      val nodeName = d.data.name
      if (js.isUndefined(nodeName) || nodeName == null) "Unnamed Node" 
      else nodeName.toString
    })
}