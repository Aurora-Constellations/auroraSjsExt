package org.aurora

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import org.scalajs.dom
import typings.d3.mod as d3

// --- THE FIX: Safe JS extraction extension ---
// This safely filters out BOTH JS undefined and JS null
extension [T](undefOr: js.UndefOr[T])
  def toSafeOption: Option[T] =
    if js.isUndefined(undefOr) || undefOr.asInstanceOf[js.Any] == null then None
    else Some(undefOr.asInstanceOf[T])
// ---------------------------------------------

// 1. Strictly Typed Facades for JavaScript Interop
@js.native
trait ElkPoint extends js.Object:
  val x: Double
  val y: Double

@js.native
trait ElkSection extends js.Object:
  val startPoint: ElkPoint
  val endPoint: ElkPoint
  val bendPoints: js.UndefOr[js.Array[ElkPoint]]

@js.native
trait ElkEdgeWithAbs extends js.Object:
  val id: js.UndefOr[String]
  var parentAbsX: Double
  var parentAbsY: Double
  val sections: js.UndefOr[js.Array[ElkSection]]

@js.native
trait D3RawData extends js.Object:
  val name: js.UndefOr[String]
  val nodeType: js.UndefOr[String]
  val x: js.UndefOr[Double]
  val y: js.UndefOr[Double]
  val width: js.UndefOr[Double]
  val height: js.UndefOr[Double]
  val edges: js.UndefOr[js.Array[ElkEdgeWithAbs]]

@js.native
trait D3AugmentedNode extends js.Object:
  var absoluteX: Double
  var absoluteY: Double
  val data: D3RawData
  val parent: js.UndefOr[D3AugmentedNode]
  def descendants(): js.Array[D3AugmentedNode]
  def links(): js.Array[js.Any]
  def each(callback: js.Function1[D3AugmentedNode, Unit]): Unit


// 2. The Reusable Renderer Class
class D3Renderer(containerSelector: String):

  private def getNodeColor(nodeType: String): String = nodeType match {
    case "Reference"                => "#61afef"
    case "Coordinate"               => "#98c379"
    case "NormalNarrative"          => "#abb2bf"
    case "UrgentNarrative"          => "#e06c75"
    case "DraftNarrative"           => "#d19a66"
    case "UrgentCompletedNarrative" => "#c678dd"
    case "DraftCompletedNarrative"  => "#56b6c2"
    case _                          => "#ffffff"
  }

  private def getEdgeColor(edgeType: String): String = edgeType match {
    case "DraftEdge"    => "#e5c07b"
    case "UrgentEdge"   => "#e06c75"
    case "NegativeEdge" => "#e06c75"
    case _              => "#555555"
  }

  private def getEdgeDash(edgeType: String): String =
    if (edgeType == "NegativeEdge") "6, 6" else "none"

  def render(data: js.Any): Unit = {
    val container = d3.select(containerSelector).asInstanceOf[js.Dynamic]
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

    val zoom = d3.zoom().asInstanceOf[js.Dynamic].on("zoom", (e: dom.Event) => {
      g.attr("transform", e.asInstanceOf[js.Dynamic].transform)
    })
    svg.call(zoom)

    val root = d3.hierarchy[js.Any](data).asInstanceOf[D3AugmentedNode]

    // Utilizing toSafeOption gracefully catches the root node's null parent
    root.each { (d: D3AugmentedNode) =>
      val parentX = d.parent.toSafeOption.map(_.absoluteX).getOrElse(0.0)
      val parentY = d.parent.toSafeOption.map(_.absoluteY).getOrElse(0.0)
      
      d.absoluteX = parentX + d.data.x.toSafeOption.getOrElse(0.0)
      d.absoluteY = parentY + d.data.y.toSafeOption.getOrElse(0.0)
    }

    val nodes = root.descendants().toList.filter(d => d.parent.toSafeOption.isDefined)
    val allEdges = js.Array[ElkEdgeWithAbs]()

    root.each { (d: D3AugmentedNode) =>
      d.data.edges.toSafeOption.map(_.toList).getOrElse(List.empty).foreach { (e: ElkEdgeWithAbs) =>
        e.parentAbsX = d.absoluteX
        e.parentAbsY = d.absoluteY
        allEdges.push(e)
      }
    }

    val lineGenerator = d3.line().asInstanceOf[js.Dynamic]
      .x((p: ElkPoint) => p.x)
      .y((p: ElkPoint) => p.y)

    g.selectAll(".link")
      .data(allEdges)
      .enter()
      .append("path")
      .attr("class", "link")
      .style("fill", "none")
      .style("stroke-width", "2px")
      .style("stroke", (d: ElkEdgeWithAbs) => {
        val edgeType = d.id.toSafeOption.getOrElse("").split("%%").lastOption.getOrElse("NormalEdge")
        getEdgeColor(edgeType)
      })
      .style("stroke-dasharray", (d: ElkEdgeWithAbs) => {
        val edgeType = d.id.toSafeOption.getOrElse("").split("%%").lastOption.getOrElse("NormalEdge")
        getEdgeDash(edgeType)
      })
      .attr("d", (d: ElkEdgeWithAbs) => {
        d.sections.toSafeOption.map(_.toList).getOrElse(List.empty).headOption.map { section =>
          val pX = d.parentAbsX
          val pY = d.parentAbsY
          
          val points = js.Array[ElkPoint]()
          points.push(js.Dictionary("x" -> (section.startPoint.x + pX), "y" -> (section.startPoint.y + pY)).asInstanceOf[ElkPoint])
          
          section.bendPoints.toSafeOption.map(_.toList).getOrElse(List.empty).foreach { bp =>
            points.push(js.Dictionary("x" -> (bp.x + pX), "y" -> (bp.y + pY)).asInstanceOf[ElkPoint])
          }
          
          points.push(js.Dictionary("x" -> (section.endPoint.x + pX), "y" -> (section.endPoint.y + pY)).asInstanceOf[ElkPoint])
          
          val pathString = lineGenerator(points).asInstanceOf[String]
          if (pathString != null) pathString else ""
        }.getOrElse("")
      })

    val nodeSelection = g.selectAll(".node")
      .data(nodes.toJSArray)
      .enter()
      .append("g")
      .attr("transform", (d: D3AugmentedNode) => s"translate(${d.absoluteX}, ${d.absoluteY})")

    nodeSelection.append("rect")
      .attr("width", (d: D3AugmentedNode) => d.data.width.toSafeOption.getOrElse(0.0))
      .attr("height", (d: D3AugmentedNode) => d.data.height.toSafeOption.getOrElse(0.0))
      .attr("rx", 6)
      .attr("ry", 6)
      .style("fill", (d: D3AugmentedNode) => getNodeColor(d.data.nodeType.toSafeOption.getOrElse("Unknown")))
      .style("stroke", "#333")
      .style("stroke-width", "2px")

    nodeSelection.append("text")
      .attr("x", (d: D3AugmentedNode) => d.data.width.toSafeOption.getOrElse(0.0) / 2)
      .attr("y", (d: D3AugmentedNode) => d.data.height.toSafeOption.getOrElse(0.0) / 2)
      .attr("dy", "4px")
      .attr("text-anchor", "middle")
      .style("fill", "#1e1e1e")
      .style("font-size", "12px")
      .style("font-weight", "bold")
      .text((d: D3AugmentedNode) => d.data.name.toSafeOption.getOrElse("Unknown"))
  }