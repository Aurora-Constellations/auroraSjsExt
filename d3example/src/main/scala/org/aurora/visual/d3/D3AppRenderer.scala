package org.aurora.visual.d3

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import org.scalajs.dom
import org.aurora.sjsast.utils.{NarrativeType, Qualifier}
import typings.d3.mod as d3

// Care scenario renderer for the end-user layout (`sbt openUI`): page shell with a header, zoom controls,
// colour key, tooltips and empty state. The developer layout keeps D3Renderer. Shares the ELK/D3 facades
// declared in D3Renderer.scala.
class D3AppRenderer(val containerSelector: String):

    // Node currently linked to the editor cursor / last click; survives re-renders
    private var highlightedId: Option[String] = None
    private var highlightRings: Option[js.Dynamic] = None
    private var drawnNodes: List[D3AugmentedNode] = Nil

    // Title of the diagram on screen; a new title means a different patient, so the view is re-fitted
    private var currentTitle: Option[String] = None
    // True until the user pans or zooms by hand; while set, the diagram re-fits whenever the view is resized
    private var autoFit = true
    private var contentBounds: Option[(Double, Double, Double, Double)] = None

    // Only available inside a VS Code webview; absent when opened in a plain browser (e.g. the Vite dev server)
    private lazy val vscodeApi: Option[js.Dynamic] =
        if (js.typeOf(js.Dynamic.global.acquireVsCodeApi) == "function")
            Some(js.Dynamic.global.acquireVsCodeApi())
        else None

    // Draws the page shell (header, empty state, key) and tells the extension the webview is listening,
    // so it can (re)send the current diagram
    def notifyReady(): Unit = {
        shell
        vscodeApi.foreach(_.postMessage(js.Dynamic.literal(command = "ready")))
    }

    // --- Display vocabulary -------------------------------------------------------------

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

    // Plain-language names for node types, used by the key and tooltips; also fixes the key's order
    private val nodeTypeLabels: List[(String, String)] = List(
        "Reference"                           -> "Issue",
        "Coordinate"                          -> "Order",
        NarrativeType.Normal.elkType          -> "Narrative",
        NarrativeType.Draft.elkType           -> "Draft narrative",
        NarrativeType.Urgent.elkType          -> "Urgent narrative",
        NarrativeType.DraftCompleted.elkType  -> "Draft narrative, completed",
        NarrativeType.UrgentCompleted.elkType -> "Urgent narrative, completed"
    )

    private def nodeTypeLabel(nodeType: String): String =
        nodeTypeLabels.toMap.getOrElse(nodeType, nodeType)

    private val qualifierLabels: List[(String, String)] = List(
        Qualifier.Draft.elkType    -> "Draft (?)",
        Qualifier.Urgent.elkType   -> "Urgent (!)",
        Qualifier.Negative.elkType -> "Negated (~)"
    )

    private def getQuColor(nodeQualifier: String): String = nodeQualifier match {
        case Qualifier.Draft.elkType    => "#d4a72c"
        case Qualifier.Urgent.elkType   => "#e06c75"
        case Qualifier.Negative.elkType => "#e06c75"
        case _                          => "var(--ap-edge)"
    }

    // Nodes only get a border when the source carries a ?, ! or ~ qualifier
    private def getNodeStroke(nodeQualifier: String): String =
        if (nodeQualifier == Qualifier.Normal.elkType) "none"
        else getQuColor(nodeQualifier)

    private def getEdgeDash(edgeType: String): String =
        if (edgeType == Qualifier.Negative.elkType) "6, 5" else "none"

    private def edgeQualifier(e: ElkEdgeWithAbs): String =
        e.id.toSafeOption
            .getOrElse("")
            .split("%%")
            .lastOption
            .filter(q => Qualifier.values.exists(_.elkType == q))
            .getOrElse(Qualifier.Normal.elkType)

    // Deeper shade of a node's fill, shown while the node is hovered
    private val HoverDarkenFactor = 0.6

    private def parseHex(color: String): Option[Seq[Int]] =
        if (!color.matches("#[0-9a-fA-F]{6}")) None
        else Some((0 until 3).map(i => Integer.parseInt(color.substring(1 + i * 2, 3 + i * 2), 16)))

    private def getHoverColor(baseColor: String): String =
        // Same maths as d3-color's darker(): scale each RGB channel by 0.7^k
        parseHex(baseColor).fold(baseColor) { channels =>
            val scale = math.pow(0.7, HoverDarkenFactor)
            "#" + channels.map(c => f"${math.round(c * scale).toInt}%02x").mkString
        }

    // Dark text on light fills, white text on saturated ones (e.g. urgent red)
    private def getLabelColor(fill: String): String =
        parseHex(fill).fold("#1e1e1e") { channels =>
            val Seq(r, g, b) = channels.map { c =>
                val s = c / 255.0
                if (s <= 0.03928) s / 12.92 else math.pow((s + 0.055) / 1.055, 2.4)
            }
            val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
            if (luminance < 0.3) "#ffffff" else "#1e1e1e"
        }

    // What the reader sees on a node: narrative markers (??, !!, ...) and the closing ; are DSL syntax,
    // and underscores are word separators. The tooltip still shows the exact source name.
    private def displayLabel(name: String, nodeType: String): String = {
        val text =
            if (nodeType.endsWith("Narrative"))
                NarrativeType.values
                    .find(t => name.startsWith(t.prefix))
                    .map(t => name.drop(t.prefix.length))
                    .getOrElse(name)
                    .trim
                    .stripSuffix(";")
                    .trim
            else name.replace('_', ' ')
        if (text.isEmpty) name else text
    }

    private def escapeHtml(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    // --- Static page shell (built once, reused by every render) ------------------------

    private case class Shell(
        app: js.Dynamic,
        title: js.Dynamic,
        stage: dom.Element,
        svg: js.Dynamic,
        viewport: js.Dynamic,
        linkLayer: js.Dynamic,
        nodeLayer: js.Dynamic,
        legendBody: js.Dynamic,
        tooltip: js.Dynamic,
        zoom: js.Dynamic
    )

    private val icons = Map(
        "zoomIn"  -> """<svg viewBox="0 0 24 24"><circle cx="11" cy="11" r="7"/><path d="M11 8v6M8 11h6M20 20l-3.5-3.5"/></svg>""",
        "zoomOut" -> """<svg viewBox="0 0 24 24"><circle cx="11" cy="11" r="7"/><path d="M8 11h6M20 20l-3.5-3.5"/></svg>""",
        "fit"     -> """<svg viewBox="0 0 24 24"><path d="M4 9V5a1 1 0 0 1 1-1h4M15 4h4a1 1 0 0 1 1 1v4M20 15v4a1 1 0 0 1-1 1h-4M9 20H5a1 1 0 0 1-1-1v-4"/></svg>""",
        "empty"   -> """<svg viewBox="0 0 48 48"><rect x="6" y="8" width="14" height="9" rx="2.5"/><rect x="28" y="8" width="14" height="9" rx="2.5"/><rect x="17" y="31" width="14" height="9" rx="2.5"/><path d="M13 17v5a3 3 0 0 0 3 3h16a3 3 0 0 0 3-3v-5M24 25v6"/></svg>"""
    )

    private lazy val shell: Shell = {
        val container = dom.document.querySelector(containerSelector)
        container.innerHTML =
            s"""<div class="ap-app">
               |  <header class="ap-header">
               |    <div class="ap-title-wrap">
               |      <span class="ap-eyebrow">Patient</span>
               |      <h1 class="ap-title">No patient selected</h1>
               |    </div>
               |    <div class="ap-controls" role="toolbar" aria-label="Diagram view">
               |      <button class="ap-btn" data-action="zoomOut" title="Zoom out" aria-label="Zoom out">${icons("zoomOut")}</button>
               |      <button class="ap-btn" data-action="zoomIn" title="Zoom in" aria-label="Zoom in">${icons("zoomIn")}</button>
               |      <button class="ap-btn" data-action="fit" title="Fit to view" aria-label="Fit to view">${icons("fit")}</button>
               |    </div>
               |  </header>
               |  <div class="ap-stage">
               |    <svg class="ap-svg" role="img" aria-label="Care scenario diagram">
               |      <g class="ap-viewport"><g class="ap-links"></g><g class="ap-nodes"></g></g>
               |    </svg>
               |    <div class="ap-empty">
               |      ${icons("empty")}
               |      <div class="ap-empty-title">No care scenario yet</div>
               |      <div class="ap-empty-text">Open a patient to see their issues, orders and notes drawn here.</div>
               |    </div>
               |    <details class="ap-legend" open>
               |      <summary>Key</summary>
               |      <div class="ap-legend-body"></div>
               |    </details>
               |  </div>
               |</div>
               |<div class="ap-tooltip" role="tooltip"></div>""".stripMargin

        val app = d3.select(container).select(".ap-app").asInstanceOf[js.Dynamic]
        val svg = app.select(".ap-svg")
        val viewport = svg.select(".ap-viewport")

        val zoom = d3
            .zoom()
            .asInstanceOf[js.Dynamic]
            .scaleExtent(js.Array(0.1, 4))
            .on(
                "zoom",
                (e: js.Dynamic) => {
                    viewport.attr("transform", e.transform)
                    // Only gestures carry a source event; programmatic fits and focus moves do not
                    if (!js.isUndefined(e.sourceEvent) && e.sourceEvent != null) autoFit = false
                }
            )
        svg.call(zoom).on("dblclick.zoom", null)

        val s = Shell(
            app = app,
            title = app.select(".ap-title"),
            stage = container.querySelector(".ap-stage"),
            svg = svg,
            viewport = viewport,
            linkLayer = viewport.select(".ap-links"),
            nodeLayer = viewport.select(".ap-nodes"),
            legendBody = app.select(".ap-legend-body"),
            tooltip = d3.select(container).select(".ap-tooltip").asInstanceOf[js.Dynamic],
            zoom = zoom
        )

        app.selectAll(".ap-btn").on(
            "click",
            (e: dom.MouseEvent) =>
                e.currentTarget.asInstanceOf[dom.Element].getAttribute("data-action") match {
                    case "zoomIn" =>
                        autoFit = false
                        s.svg.transition().duration(200).call(s.zoom.scaleBy, 1.25)
                    case "zoomOut" =>
                        autoFit = false
                        s.svg.transition().duration(200).call(s.zoom.scaleBy, 0.8)
                    case _ => fitToView(s, animate = true)
                }
        )

        // Remember whether the key was collapsed, across reloads of the webview
        val legend = container.querySelector(".ap-legend").asInstanceOf[dom.html.Element]
        vscodeApi.foreach { api =>
            val saved = api.getState().asInstanceOf[js.UndefOr[js.Dynamic]].toSafeOption
            saved.flatMap(_.legendOpen.asInstanceOf[js.UndefOr[Boolean]].toSafeOption).foreach { open =>
                if (!open) legend.removeAttribute("open")
            }
            legend.addEventListener(
                "toggle",
                (_: dom.Event) => api.setState(js.Dynamic.literal(legendOpen = legend.hasAttribute("open")))
            )
        }

        // Keep the whole diagram in view as the panel is resized, until the user takes over
        val observer = js.Dynamic.newInstance(js.Dynamic.global.ResizeObserver)(
            (_: js.Any) => if (autoFit) fitToView(s, animate = false)
        )
        observer.observe(s.stage)

        s
    }

    // --- View helpers -------------------------------------------------------------------

    private def stageSize(s: Shell): (Double, Double) =
        (s.stage.clientWidth.toDouble, s.stage.clientHeight.toDouble)

    private def fitToView(s: Shell, animate: Boolean): Unit =
        contentBounds.foreach { case (minX, minY, maxX, maxY) =>
            val (w, h) = stageSize(s)
            if (w > 0 && h > 0) {
                val padding = 28.0
                val bw = math.max(maxX - minX, 1.0)
                val bh = math.max(maxY - minY, 1.0)
                val scale = math.max(0.1, math.min(1.25, math.min((w - padding * 2) / bw, (h - padding * 2) / bh)))
                val tx = (w - bw * scale) / 2 - minX * scale
                val ty = (h - bh * scale) / 2 - minY * scale
                val target = d3.zoomIdentity.translate(tx, ty).scale(scale)
                val selection = if (animate) s.svg.transition().duration(350) else s.svg
                selection.call(s.zoom.transform, target)
                autoFit = true
            }
        }

    // Pans so the given node sits in the centre of the view, zooming in only if it is too small to read
    private def focusNode(s: Shell, d: D3AugmentedNode): Unit = {
        val (w, h) = stageSize(s)
        val current = d3.zoomTransform(s.svg.node().asInstanceOf[dom.Element]).asInstanceOf[js.Dynamic].k.asInstanceOf[Double]
        val scale = math.min(2.0, math.max(current, 1.1))
        val cx = d.absoluteX + d.data.width.toSafeOption.getOrElse(0.0) / 2
        val cy = d.absoluteY + d.data.height.toSafeOption.getOrElse(0.0) / 2
        val target = d3.zoomIdentity.translate(w / 2 - cx * scale, h / 2 - cy * scale).scale(scale)
        s.svg.transition().duration(500).call(s.zoom.transform, target)
        autoFit = false
    }

    private def applyHighlight(): Unit =
        highlightRings.foreach { rings =>
            rings.classed(
                "visible",
                (d: D3AugmentedNode) => d.data.id.toSafeOption.exists(highlightedId.contains)
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

        if (enteredNewNode) matchingNode.foreach(focusNode(shell, _))
    }

    // ELK routes edges orthogonally; soften each bend into a short curve
    private def roundedPath(points: Seq[(Double, Double)], radius: Double): String =
        if (points.size < 2) ""
        else {
            val sb = new StringBuilder(s"M${points.head._1},${points.head._2}")
            points.sliding(3).foreach {
                case Seq((px, py), (cx, cy), (nx, ny)) =>
                    val d1 = math.hypot(cx - px, cy - py)
                    val d2 = math.hypot(nx - cx, ny - cy)
                    val r = math.min(radius, math.min(d1, d2) / 2)
                    if (r < 0.5) sb.append(s"L$cx,$cy")
                    else {
                        val ax = cx - (cx - px) / d1 * r
                        val ay = cy - (cy - py) / d1 * r
                        val bx = cx + (nx - cx) / d2 * r
                        val by = cy + (ny - cy) / d2 * r
                        sb.append(s"L$ax,${ay}Q$cx,$cy $bx,$by")
                    }
                case _ => ()
            }
            sb.append(s"L${points.last._1},${points.last._2}")
            sb.toString
        }

    private def edgePoints(e: ElkEdgeWithAbs): Seq[(Double, Double)] =
        e.sections.toSafeOption.flatMap(_.headOption).fold(Seq.empty[(Double, Double)]) { section =>
            val all = Seq(section.startPoint) ++
                section.bendPoints.toSafeOption.map(_.toSeq).getOrElse(Seq.empty) ++
                Seq(section.endPoint)
            all.map(p => (p.x + e.parentAbsX, p.y + e.parentAbsY))
        }

    private def edgeTouches(e: ElkEdgeWithAbs, nodeId: String): Boolean =
        e.sources.toSafeOption.exists(_.contains(nodeId)) || e.targets.toSafeOption.exists(_.contains(nodeId))

    private def renderLegend(s: Shell, nodes: List[D3AugmentedNode], edges: Seq[ElkEdgeWithAbs]): Unit = {
        val presentTypes = nodes.flatMap(_.data.nodeType.toSafeOption).toSet
        val presentQualifiers =
            nodes.flatMap(_.data.nodeQualifier.toSafeOption).toSet ++ edges.map(edgeQualifier).toSet

        val typeRows = nodeTypeLabels.filter { case (t, _) => presentTypes.contains(t) }.map { case (t, label) =>
            s"""<div class="ap-legend-row"><span class="ap-swatch" style="background:${getNodeColor(t)}"></span>$label</div>"""
        }
        val qualifierRows = qualifierLabels.filter { case (q, _) => presentQualifiers.contains(q) }.map { case (q, label) =>
            val style = if (q == Qualifier.Negative.elkType) "dashed" else "solid"
            s"""<div class="ap-legend-row"><span class="ap-swatch ap-swatch-line" style="border-color:${getQuColor(q)};border-bottom-style:$style"></span>$label</div>"""
        }
        val qualifierSection =
            if (qualifierRows.isEmpty) ""
            else s"""<div class="ap-legend-group">Qualifiers</div>${qualifierRows.mkString}"""

        s.legendBody.html(typeRows.mkString + qualifierSection)
    }

    // --- Rendering ----------------------------------------------------------------------

    def render(data: js.Any): Unit = render(data, None)

    def render(data: js.Any, title: Option[String]): Unit = {
        val s = shell
        val isNewDocument = title.isEmpty || title != currentTitle
        currentTitle = title
        s.title.text(title.getOrElse("Untitled"))

        s.linkLayer.selectAll("*").remove()
        s.nodeLayer.selectAll("*").remove()

        val root =
            d3.hierarchy[js.Any](data)
                .asInstanceOf[D3AugmentedNode]

        root.each { (d: D3AugmentedNode) =>
            val parentX = d.parent.toSafeOption.map(_.absoluteX).getOrElse(0.0)
            val parentY = d.parent.toSafeOption.map(_.absoluteY).getOrElse(0.0)
            d.absoluteX = parentX + d.data.x.toSafeOption.getOrElse(0.0)
            d.absoluteY = parentY + d.data.y.toSafeOption.getOrElse(0.0)
        }

        val nodes =
            root.descendants()
                .toList
                .filter(d => d.parent.toSafeOption.isDefined)

        val allEdges = js.Array[ElkEdgeWithAbs]()
        root.each { (d: D3AugmentedNode) =>
            d.data.edges.toSafeOption.map(_.toList).getOrElse(List.empty).foreach { (e: ElkEdgeWithAbs) =>
                e.parentAbsX = d.absoluteX
                e.parentAbsY = d.absoluteY
                allEdges.push(e)
            }
        }

        contentBounds =
            if (nodes.isEmpty) None
            else {
                val xs = nodes.flatMap(n => Seq(n.absoluteX, n.absoluteX + n.data.width.toSafeOption.getOrElse(0.0)))
                val ys = nodes.flatMap(n => Seq(n.absoluteY, n.absoluteY + n.data.height.toSafeOption.getOrElse(0.0)))
                Some((xs.min, ys.min, xs.max, ys.max))
            }

        s.app.classed("has-diagram", nodes.nonEmpty)

        val links = s.linkLayer
            .selectAll(".ap-link")
            .data(allEdges)
            .enter()
            .append("path")
            .attr("class", "ap-link")
            .style("stroke", (d: ElkEdgeWithAbs) => getQuColor(edgeQualifier(d)))
            .style("stroke-dasharray", (d: ElkEdgeWithAbs) => getEdgeDash(edgeQualifier(d)))
            .attr("d", (d: ElkEdgeWithAbs) => roundedPath(edgePoints(d), 8))

        val nodeSelection = s.nodeLayer
            .selectAll(".ap-node")
            .data(nodes.toJSArray)
            .enter()
            .append("g")
            .attr("class", "ap-node")
            .attr("transform", (d: D3AugmentedNode) => s"translate(${d.absoluteX}, ${d.absoluteY})")
            .attr("tabindex", 0)
            .attr("role", "button")
            .attr(
                "aria-label",
                (d: D3AugmentedNode) =>
                    s"${nodeTypeLabel(d.data.nodeType.toSafeOption.getOrElse(""))}: ${d.data.name.toSafeOption.getOrElse("")}"
            )

        def nodeFill(d: D3AugmentedNode) = getNodeColor(d.data.nodeType.toSafeOption.getOrElse("Unknown"))
        def nodeQualifier(d: D3AugmentedNode) = d.data.nodeQualifier.toSafeOption.getOrElse(Qualifier.Normal.elkType)

        def showTooltip(e: dom.MouseEvent, d: D3AugmentedNode): Unit = {
            val nodeType = d.data.nodeType.toSafeOption.getOrElse("Unknown")
            val qualifier = qualifierLabels.toMap.get(nodeQualifier(d))
            val canJump = vscodeApi.isDefined && d.data.range.toSafeOption.isDefined
            s.tooltip
                .html(
                    s"""<div class="ap-tt-type"><span class="ap-tt-dot" style="background:${nodeFill(d)}"></span>${escapeHtml(nodeTypeLabel(nodeType))}</div>
                       |<div class="ap-tt-name">${escapeHtml(d.data.name.toSafeOption.getOrElse("Unknown"))}</div>
                       |${qualifier.fold("")(q => s"""<div class="ap-tt-flag">${escapeHtml(q)}</div>""")}
                       |${if (canJump) """<div class="ap-tt-hint">Click to show in the orders</div>""" else ""}""".stripMargin
                )
                .classed("visible", true)
            moveTooltip(e)
        }

        def moveTooltip(e: dom.MouseEvent): Unit = {
            val tip = s.tooltip.node().asInstanceOf[dom.html.Element]
            // Flip to the other side of the cursor near the right/bottom edges so the tooltip stays on screen
            val left =
                if (e.clientX + 16 + tip.offsetWidth > dom.window.innerWidth) e.clientX - 12 - tip.offsetWidth
                else e.clientX + 16
            val top =
                if (e.clientY + 16 + tip.offsetHeight > dom.window.innerHeight) e.clientY - 12 - tip.offsetHeight
                else e.clientY + 16
            s.tooltip.style("left", s"${math.max(4.0, left)}px").style("top", s"${math.max(4.0, top)}px")
        }

        // Emphasise a node's own connections and fade the rest while it is hovered
        def setFocus(nodeId: Option[String]): Unit = {
            s.app.classed("is-focusing", nodeId.isDefined)
            links.classed("is-related", (e: ElkEdgeWithAbs) => nodeId.exists(edgeTouches(e, _)))
        }

        def selectNode(d: D3AugmentedNode): Unit = {
            // Select the matching text in the editor and highlight this node straight away
            highlightedId = d.data.id.toSafeOption
            applyHighlight()
            vscodeApi.foreach(
                _.postMessage(
                    js.Dynamic.literal(
                        command = "nodeClicked",
                        id = d.data.id,
                        range = d.data.range
                    )
                )
            )
            focusNode(s, d)
        }

        nodeSelection
            .on(
                "mouseenter",
                (e: dom.MouseEvent, d: D3AugmentedNode) => {
                    d3.select(e.currentTarget.asInstanceOf[dom.Element])
                        .select(".ap-node-box")
                        .asInstanceOf[js.Dynamic]
                        .transition()
                        .duration(150)
                        .style("fill", getHoverColor(nodeFill(d)))
                    setFocus(d.data.id.toSafeOption)
                    showTooltip(e, d)
                }
            )
            .on("mousemove", (e: dom.MouseEvent, _: D3AugmentedNode) => moveTooltip(e))
            .on(
                "mouseleave",
                (e: dom.MouseEvent, d: D3AugmentedNode) => {
                    d3.select(e.currentTarget.asInstanceOf[dom.Element])
                        .select(".ap-node-box")
                        .asInstanceOf[js.Dynamic]
                        .transition()
                        .duration(250)
                        .style("fill", nodeFill(d))
                    setFocus(None)
                    s.tooltip.classed("visible", false)
                }
            )
            .on("click", (_: dom.MouseEvent, d: D3AugmentedNode) => selectNode(d))
            .on(
                "keydown",
                (e: dom.KeyboardEvent, d: D3AugmentedNode) =>
                    if (e.key == "Enter" || e.key == " ") {
                        e.preventDefault()
                        selectNode(d)
                    }
            )

        nodeSelection
            .append("rect")
            .attr("class", "ap-node-box")
            .attr("width", (d: D3AugmentedNode) => d.data.width.toSafeOption.getOrElse(0.0))
            .attr("height", (d: D3AugmentedNode) => d.data.height.toSafeOption.getOrElse(0.0))
            .attr("rx", 8)
            .attr("ry", 8)
            .style("fill", (d: D3AugmentedNode) => nodeFill(d))
            .style("stroke", (d: D3AugmentedNode) => getNodeStroke(nodeQualifier(d)))
            .style("stroke-dasharray", (d: D3AugmentedNode) => getEdgeDash(nodeQualifier(d)))
            .style("stroke-width", "2px")

        // Outline drawn just outside the node, separate from the qualifier border so both can show at once
        val ringPadding = 5.0
        highlightRings = Some(
            nodeSelection
                .append("rect")
                .attr("class", "ap-highlight-ring")
                .attr("x", -ringPadding)
                .attr("y", -ringPadding)
                .attr("width", (d: D3AugmentedNode) => d.data.width.toSafeOption.getOrElse(0.0) + ringPadding * 2)
                .attr("height", (d: D3AugmentedNode) => d.data.height.toSafeOption.getOrElse(0.0) + ringPadding * 2)
                .attr("rx", 11)
                .attr("ry", 11)
                .asInstanceOf[js.Dynamic]
        )
        drawnNodes = nodes
        applyHighlight()

        nodeSelection
            .append("text")
            .attr("class", "ap-node-label")
            .attr("x", (d: D3AugmentedNode) => d.data.width.toSafeOption.getOrElse(0.0) / 2)
            .attr("y", (d: D3AugmentedNode) => d.data.height.toSafeOption.getOrElse(0.0) / 2)
            .attr("dy", "0.35em")
            .attr("text-anchor", "middle")
            .style("fill", (d: D3AugmentedNode) => getLabelColor(nodeFill(d)))
            .text((d: D3AugmentedNode) =>
                displayLabel(d.data.name.toSafeOption.getOrElse("Unknown"), d.data.nodeType.toSafeOption.getOrElse(""))
            )

        renderLegend(s, nodes, allEdges.toSeq)

        // A different patient starts from an overview; re-renders of the same one (e.g. on save) keep the user's view
        if (isNewDocument || autoFit) fitToView(s, animate = false)
    }
