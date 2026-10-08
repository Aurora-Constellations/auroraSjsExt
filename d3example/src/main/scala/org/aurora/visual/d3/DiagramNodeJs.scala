package org.aurora.visual.d3

import com.axiom.protocol.{DiagramEdge, DiagramNode, EdgeSection, Point, SourceRange}
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

// Wire DTO -> the plain JS objects the d3 renderer reads through the `D3RawData` / `ElkEdgeWithAbs` facades.
// The renderer mutates some of them (d3 hierarchy, edge offsets), so these must be fresh objects, not frozen ones.

extension (p: Point)
  def toJS: js.Any = js.Dynamic.literal(x = p.x, y = p.y)

extension (s: EdgeSection)
  def toJS: js.Any = js.Dynamic.literal(
    startPoint = s.startPoint.toJS,
    endPoint = s.endPoint.toJS,
    bendPoints = s.bendPoints.map(_.toJS).toJSArray
  )

extension (e: DiagramEdge)
  def toJS: js.Any = js.Dynamic.literal(
    id = e.id,
    sections = e.sections.map(_.toJS).toJSArray
  )

extension (r: SourceRange)
  def toJS: js.Any = js.Dynamic.literal(
    startLine = r.startLine,
    startChar = r.startChar,
    endLine = r.endLine,
    endChar = r.endChar
  )

extension (n: DiagramNode)
  def toJS: js.Any = js.Dynamic.literal(
    name = n.name,
    nodeType = n.nodeType,
    nodeQualifier = n.nodeQualifier,
    x = n.x,
    y = n.y,
    width = n.width,
    height = n.height,
    children = n.children.map(_.toJS).toJSArray,
    edges = n.edges.map(_.toJS).toJSArray,
    id = n.id,
    range = n.range.map(_.toJS).getOrElse(js.undefined)
  )
