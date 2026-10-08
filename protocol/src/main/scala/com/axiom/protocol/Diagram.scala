package com.axiom.protocol

/** Wire types describing the diagram the extension host draws in the D3 webview.
  *
  * These are plain DTOs: they carry only what the webview needs and know nothing about the Aurora AST, ELK or d3.
  * Mapping from and to the domain / renderer types is done at the edges (see `d3example`).
  */

/** A zero-based range in the .aurora source text (same convention as VS Code positions). */
final case class SourceRange(startLine: Int, startChar: Int, endLine: Int, endChar: Int)

final case class Point(x: Double, y: Double)

/** One routed segment of an edge, relative to its owning node. */
final case class EdgeSection(startPoint: Point, endPoint: Point, bendPoints: List[Point] = Nil)

/** An edge's id encodes its qualifier after `%%` (e.g. `OC1->IC1%%DraftEdge`); the renderer derives colour and dash
  * style from it.
  */
final case class DiagramEdge(id: String, sections: List[EdgeSection] = Nil)

final case class DiagramNode(
    name: String,
    nodeType: String,
    nodeQualifier: String,
    x: Double,
    y: Double,
    width: Double,
    height: Double,
    children: List[DiagramNode] = Nil,
    edges: List[DiagramEdge] = Nil,
    id: String = "",
    range: Option[SourceRange] = None
)
