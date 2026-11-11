package org.aurora.visual.d3

import scala.scalajs.js
import org.aurora.sjsast.utils.Qualifier

// Arrowheads on the diagram's edges, shared by both renderers. They read Issue -> Order -> Narrative, general to
// specific: an issue drives its orders, and a narrative details the issue or order it belongs to.
// The data stores an order's link to its issue as order -> issue (the order references the issue), so on those
// edges the arrowhead is drawn at the start of the line instead of the end.
// Each arrowhead takes the colour of its edge's qualifier (normal, draft, urgent, negated).
object Arrowheads:

    // The qualifier an edge id ends with, e.g. "OC1%%Coordinate->IC1%%Reference%%Draft" -> "Draft"
    def qualifierOf(e: ElkEdgeWithAbs): String =
        e.id.toSafeOption
            .flatMap(_.split("%%").lastOption)
            .filter(q => Qualifier.values.exists(_.elkType == q))
            .getOrElse(Qualifier.Normal.elkType)

    // Order -> issue edges: the arrow belongs at the order's end
    def pointsToSource(e: ElkEdgeWithAbs): Boolean =
        e.targets.toSafeOption.exists(_.exists(_.endsWith("%%Reference")))

    private def markerId(prefix: String, qualifier: String) = s"$prefix-arrow-$qualifier"

    // Adds one arrowhead marker per qualifier to the svg, filled with that qualifier's edge colour
    def define(svg: js.Dynamic, prefix: String, colourOf: String => String): Unit = {
        val defs = svg.insert("defs", ":first-child")
        Qualifier.values.foreach { q =>
            defs
                .append("marker")
                .attr("id", markerId(prefix, q.elkType))
                .attr("viewBox", "0 0 10 10")
                .attr("refX", 9)
                .attr("refY", 5)
                .attr("markerWidth", 9)
                .attr("markerHeight", 9)
                // Same size whatever the line's width (e.g. while hover emphasis thickens it)
                .attr("markerUnits", "userSpaceOnUse")
                // At the start of a line the arrow turns round, so it points out of the line
                .attr("orient", "auto-start-reverse")
                .append("path")
                .attr("d", "M0,1 L10,5 L0,9 Z")
                .style("fill", colourOf(q.elkType))
        }
    }

    // Puts each edge's arrowhead on the end the Issue -> Order -> Narrative direction points to
    def apply(links: js.Dynamic, prefix: String): Unit = {
        def url(e: ElkEdgeWithAbs) = s"url(#${markerId(prefix, qualifierOf(e))})"
        links
            .attr("marker-end", (e: ElkEdgeWithAbs) => (if (pointsToSource(e)) null else url(e)): String)
            .attr("marker-start", (e: ElkEdgeWithAbs) => (if (pointsToSource(e)) url(e) else null): String)
    }
