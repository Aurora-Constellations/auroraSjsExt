package org.aurora.visual.d3

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import org.scalajs.dom
import org.aurora.sjsast.utils.{NarrativeType, Qualifier}
import typings.d3.mod as d3
import com.axiom.protocol.{SourceRange, ToExtension}

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
trait D3SourceRange extends js.Object:
    val startLine: Int
    val startChar: Int
    val endLine: Int
    val endChar: Int

@js.native
trait D3RawData extends js.Object:
    val id: js.UndefOr[String]
    val range: js.UndefOr[D3SourceRange]
    val name: js.UndefOr[String]
    val nodeType: js.UndefOr[String]
    val nodeQualifier: js.UndefOr[String]
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
// `outbound` is where the renderer reports user interaction (e.g. node clicks); the default drops them, which is what
// we want when the page is opened outside VS Code.
class D3Renderer(val containerSelector: String, outbound: ToExtension => Unit = _ => ()):

    // Node currently linked to the editor cursor / last click; survives re-renders
    private var highlightedId: Option[String] = None
    private var highlightRings: Option[js.Dynamic] = None
    private var drawnNodes: List[D3AugmentedNode] = Nil
    private var focusOnNode: Option[D3AugmentedNode => Unit] = None

    private val HighlightColor = "#0078d4"

    private def applyHighlight(): Unit =
        highlightRings.foreach { rings =>
            rings.style(
                "opacity",
                (d: D3AugmentedNode) =>
                    if (d.data.id.toSafeOption.exists(highlightedId.contains)) "1" else "0"
            )
        }

    // Highlights the node whose source text contains the given editor position (zero-based line/character)
    def highlightAt(line: Int, character: Int): Unit = {
        def atOrAfter(l: Int, c: Int, refLine: Int, refChar: Int) = l > refLine || (l == refLine && c >= refChar)

        val matchingNode = drawnNodes.find { n =>
            n.data.range.toSafeOption.exists { r =>
                atOrAfter(line, character, r.startLine, r.startChar) &&
                atOrAfter(r.endLine, r.endChar, line, character)
            }
        }
        val matchingId = matchingNode.flatMap(_.data.id.toSafeOption)

        // Only re-centre when the cursor enters a different node; this also skips the echo of a node click
        val enteredNewNode = matchingId.isDefined && matchingId != highlightedId

        highlightedId = matchingId
        applyHighlight()

        if (enteredNewNode) {
            for (node <- matchingNode; focus <- focusOnNode) focus(node)
        }
    }

    private def getNodeColor(nodeType: String): String = nodeType match {
        case "Reference"                           => "#6fbbfa"
        case "Coordinate"                          => "#a4f56a"
        case NarrativeType.Normal.elkType          => "#abb2bf"
        case NarrativeType.Urgent.elkType          => "#fa2a3b"
        case NarrativeType.Draft.elkType           => "#f8e17c"
        case NarrativeType.UrgentCompleted.elkType => "#c678dd" // Different saturated green with red outline?
        case NarrativeType.DraftCompleted.elkType  => "#56b6c2" // As green but no outline
        case _                                     => "#ffffff"
    }

    private def getQuColor(nodeQualifier: String): String = nodeQualifier match {
        case Qualifier.Draft.elkType    => "#e5c07b"
        case Qualifier.Urgent.elkType   => "#e06c75"
        case Qualifier.Negative.elkType => "#e06c75"
        case _                          => "#555555"
    }

    // Nodes only get a border when the source carries a ?, ! or ~ qualifier
    private def getNodeStroke(nodeQualifier: String): String =
        if (nodeQualifier == Qualifier.Normal.elkType) "none"
        else getQuColor(nodeQualifier)

    // Deeper shade of a node's fill, shown while the node is hovered
    private val HoverDarkenFactor = 0.6

    private def getHoverColor(baseColor: String): String = {
        // Same maths as d3-color's darker(): scale each RGB channel by 0.7^k
        val isHex = baseColor.matches("#[0-9a-fA-F]{6}")
        if (!isHex) baseColor
        else {
            val scale = math.pow(0.7, HoverDarkenFactor)
            val channels = (0 until 3).map { i =>
                val v = Integer.parseInt(baseColor.substring(1 + i * 2, 3 + i * 2), 16)
                math.round(v * scale).toInt
            }
            "#" + channels.map(c => f"$c%02x").mkString
        }
    }

    private def getEdgeDash(edgeType: String): String =
        if (edgeType == Qualifier.Negative.elkType) "6, 6" else "none"

    def render(data: js.Any): Unit = {

    val container =
        d3.select(containerSelector).asInstanceOf[js.Dynamic]

    container.selectAll("*").remove()



    // --- Tooltip ---
    val tooltip = d3
        .select("body")
        .append("div")
        .style("position", "absolute")
        .style("visibility", "hidden")
        .style("background-color", "rgba(20, 20, 20, 0.9)")
        .style("color", "#fff")
        .style("padding", "8px 12px")
        .style("border-radius", "6px")
        .style("border", "1px solid #555")
        .style("font-size", "12px")
        .style("font-family", "sans-serif")
        .style("pointer-events", "none")
        .style("z-index", "1000")
        .style("box-shadow", "0px 4px 6px rgba(0,0,0,0.3)")



    val width = dom.window.innerWidth.toDouble
    val height = dom.window.innerHeight.toDouble

    val svg = container
        .append("svg")
        .attr("width", "100%")
        .attr("height", "100vh")
        .attr("viewBox", s"0 0 $width $height")
        .style("background-color", "#f1f1f1")
        .style("font-family", "sans-serif")
        .asInstanceOf[js.Dynamic]


    val g =
        svg.append("g").asInstanceOf[js.Dynamic]


    val zoom = d3
        .zoom()
        .asInstanceOf[js.Dynamic]
        .on(
        "zoom",
        (e: dom.Event) => {
            g.attr(
            "transform",
            e.asInstanceOf[js.Dynamic].transform
            )
        }
        )

    svg.call(zoom)

    val root =
        d3.hierarchy[js.Any](data)
        .asInstanceOf[D3AugmentedNode]

    root.each { (d: D3AugmentedNode) =>

        val parentX =
        d.parent.toSafeOption
            .map(_.absoluteX)
            .getOrElse(0.0)

        val parentY =
        d.parent.toSafeOption
            .map(_.absoluteY)
            .getOrElse(0.0)

        d.absoluteX =
        parentX +
            d.data.x.toSafeOption.getOrElse(0.0)

        d.absoluteY =
        parentY +
            d.data.y.toSafeOption.getOrElse(0.0)
    }

    val nodes =
        root.descendants()
        .toList
        .filter(d => d.parent.toSafeOption.isDefined)

    val allEdges =
        js.Array[ElkEdgeWithAbs]()

    root.each { (d: D3AugmentedNode) =>

        d.data.edges.toSafeOption
        .map(_.toList)
        .getOrElse(List.empty)
        .foreach { (e: ElkEdgeWithAbs) =>

            e.parentAbsX = d.absoluteX
            e.parentAbsY = d.absoluteY

            allEdges.push(e)
        }
    }


    val lineGenerator = d3
        .line()
        .asInstanceOf[js.Dynamic]
        .x((p: ElkPoint) => p.x)
        .y((p: ElkPoint) => p.y)



    g.selectAll(".link")
        .data(allEdges)
        .enter()
        .append("path")
        .attr("class", "link")
        .style("fill", "none")
        .style("stroke-width", "2px")
        .style(
        "stroke",
        (d: ElkEdgeWithAbs) => {

            val edgeType =
            d.id.toSafeOption
                .getOrElse("")
                .split("%%")
                .lastOption
                .getOrElse(Qualifier.Normal.elkType)

            getQuColor(edgeType)
        }
        )
        .style(
        "stroke-dasharray",
        (d: ElkEdgeWithAbs) => {

            val edgeType =
            d.id.toSafeOption
                .getOrElse("")
                .split("%%")
                .lastOption
                .getOrElse(Qualifier.Normal.elkType)

            getEdgeDash(edgeType)
        }
        )
        .attr(
        "d",
        (d: ElkEdgeWithAbs) => {

            d.sections.toSafeOption
            .map(_.toList)
            .getOrElse(List.empty)
            .headOption
            .map { section =>

                val pX = d.parentAbsX
                val pY = d.parentAbsY

                val points =
                js.Array[ElkPoint]()

                points.push(
                js.Dictionary(
                    "x" -> (section.startPoint.x + pX),
                    "y" -> (section.startPoint.y + pY)
                ).asInstanceOf[ElkPoint]
                )

                section.bendPoints.toSafeOption
                .map(_.toList)
                .getOrElse(List.empty)
                .foreach { bp =>

                    points.push(
                    js.Dictionary(
                        "x" -> (bp.x + pX),
                        "y" -> (bp.y + pY)
                    ).asInstanceOf[ElkPoint]
                    )
                }

                points.push(
                js.Dictionary(
                    "x" -> (section.endPoint.x + pX),
                    "y" -> (section.endPoint.y + pY)
                ).asInstanceOf[ElkPoint]
                )

                val pathString =
                lineGenerator(points)
                    .asInstanceOf[String]

                if (pathString != null)
                pathString
                else
                ""
            }
            .getOrElse("")
        }
        )

    // Pans and zooms the diagram so the given node sits in the centre of the view
    def focusNode(d: D3AugmentedNode): Unit = {

        val scale = 1.5

        val targetX =
        (width / 2) -
            (
            d.absoluteX +
                (
                d.data.width.toSafeOption
                    .getOrElse(0.0) / 2
                )
            ) * scale

        val targetY =
        (height / 2) -
            (
            d.absoluteY +
                (
                d.data.height.toSafeOption
                    .getOrElse(0.0) / 2
                )
            ) * scale

        val newTransform =
        d3.zoomIdentity
            .translate(
            targetX,
            targetY
            )
            .scale(scale)

        svg.transition()
        .duration(750)
        .call(
            zoom.transform,
            newTransform
        )
    }
    focusOnNode = Some(focusNode)

    val nodeSelection = g
        .selectAll(".node")
        .data(nodes.toJSArray)
        .enter()
        .append("g")
        .attr(
        "transform",
        (d: D3AugmentedNode) =>
            s"translate(${d.absoluteX}, ${d.absoluteY})"
        )
        .style("cursor", "pointer")
        .on(
        "mouseover",
        (e: dom.MouseEvent, d: D3AugmentedNode) => {

            d3.select(
            e.currentTarget.asInstanceOf[dom.Element]
            )
            .select("rect")
            .asInstanceOf[js.Dynamic]
            .transition()
            .duration(150)
            .style(
                "fill",
                getHoverColor(
                getNodeColor(
                    d.data.nodeType.toSafeOption.getOrElse("Unknown")
                )
                )
            )

            val nType =
            d.data.nodeType.toSafeOption
                .getOrElse("Unknown")
                .replace(
                "Narrative",
                " Narrative"
                )

            val nName =
            d.data.name.toSafeOption
                .getOrElse("Unknown")

            tooltip
            .html(
                s"""
                |<strong>Type:</strong> $nType
                |<br/>
                |<strong>Name:</strong> $nName
                |""".stripMargin
            )
            .style(
                "visibility",
                "visible"
            )
        }
        )
        .on(
        "mousemove",
        (e: dom.MouseEvent, d: D3AugmentedNode) => {

            tooltip
            .style(
                "top",
                s"${e.pageY + 15}px"
            )
            .style(
                "left",
                s"${e.pageX + 15}px"
            )
        }
        )
        .on(
        "mouseout",
        (e: dom.MouseEvent, d: D3AugmentedNode) => {

            d3.select(
            e.currentTarget.asInstanceOf[dom.Element]
            )
            .select("rect")
            .asInstanceOf[js.Dynamic]
            .transition()
            .duration(250)
            .style(
                "fill",
                getNodeColor(
                d.data.nodeType.toSafeOption.getOrElse("Unknown")
                )
            )
            .style(
                "stroke",
                getNodeStroke(
                d.data.nodeQualifier.toSafeOption
                    .getOrElse(
                    Qualifier.Normal.elkType
                    )
                )
            )
            .style(
                "stroke-width",
                "2px"
            )

            tooltip.style(
            "visibility",
            "hidden"
            )
        }
        )
        .on(
        "click",
        (e: dom.MouseEvent, d: D3AugmentedNode) => {

            // Select the matching text in the editor and highlight this node straight away
            highlightedId = d.data.id.toSafeOption
            applyHighlight()
            // Only nodes that map back to source text can be selected in the editor
            for (id <- d.data.id.toSafeOption; r <- d.data.range.toSafeOption)
                outbound(ToExtension.NodeClicked(id, SourceRange(r.startLine, r.startChar, r.endLine, r.endChar)))

            focusNode(d)
        }
        )

    nodeSelection
        .append("rect")
        .attr(
        "width",
        (d: D3AugmentedNode) =>
            d.data.width.toSafeOption
            .getOrElse(0.0)
        )
        .attr(
        "height",
        (d: D3AugmentedNode) =>
            d.data.height.toSafeOption
            .getOrElse(0.0)
        )
        .attr("rx", 6)
        .attr("ry", 6)
        .style(
        "fill",
        (d: D3AugmentedNode) =>
            getNodeColor(
            d.data.nodeType.toSafeOption
                .getOrElse("Unknown")
            )
        )
        .style(
        "stroke",
        (d: D3AugmentedNode) =>
            getNodeStroke(
            d.data.nodeQualifier.toSafeOption
                .getOrElse(
                Qualifier.Normal.elkType
                )
            )
        )
        .style(
        "stroke-dasharray",
        (d: D3AugmentedNode) => {

            val qual =
            d.data.nodeQualifier.toSafeOption
                .getOrElse(
                Qualifier.Normal.elkType
                )

            getEdgeDash(qual)
        }
        )
        .style(
        "stroke-width",
        "2px"
        )

    // Outline drawn just outside the node, separate from the qualifier border so both can show at once
    val ringPadding = 5.0
    highlightRings = Some(
        nodeSelection
        .append("rect")
        .attr("class", "highlight-ring")
        .attr("x", -ringPadding)
        .attr("y", -ringPadding)
        .attr(
            "width",
            (d: D3AugmentedNode) =>
            d.data.width.toSafeOption.getOrElse(0.0) + ringPadding * 2
        )
        .attr(
            "height",
            (d: D3AugmentedNode) =>
            d.data.height.toSafeOption.getOrElse(0.0) + ringPadding * 2
        )
        .attr("rx", 9)
        .attr("ry", 9)
        .style("fill", "none")
        .style("stroke", HighlightColor)
        .style("stroke-width", "3px")
        .style("pointer-events", "none")
        .style("opacity", "0")
        .asInstanceOf[js.Dynamic]
    )
    drawnNodes = nodes
    applyHighlight()

    nodeSelection
        .append("text")
        .attr(
        "x",
        (d: D3AugmentedNode) =>
            d.data.width.toSafeOption
            .getOrElse(0.0) / 2
        )
        .attr(
        "y",
        (d: D3AugmentedNode) =>
            d.data.height.toSafeOption
            .getOrElse(0.0) / 2
        )
        .attr(
        "dy",
        "4px"
        )
        .attr(
        "text-anchor",
        "middle"
        )
        .style(
        "fill",
        "#1e1e1e"
        )
        .style(
        "font-size",
        "12px"
        )
        .style(
        "font-weight",
        "bold"
        )
        .text(
        (d: D3AugmentedNode) =>
            d.data.name.toSafeOption
            .getOrElse("Unknown")
        )

    }