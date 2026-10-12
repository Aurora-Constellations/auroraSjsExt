package org.aurora.visual.d3

import scala.collection.mutable
import scala.scalajs.js
import org.aurora.sjsast.GenAst
import org.aurora.sjsast.utils.NarrativeType

/** A zero-based range in the .aurora source text (same convention as VS Code positions). */
case class SourceRange(startLine: Int, startChar: Int, endLine: Int, endChar: Int):
  def toJS: js.Any = js.Dictionary[Int](
    "startLine" -> startLine,
    "startChar" -> startChar,
    "endLine" -> endLine,
    "endChar" -> endChar
  )

/** Maps drawable diagram nodes back to the text in the editor that declares them. */
object SourceRanges:

  /** Keyed by ELK node id (e.g. `cxr%%Coordinate`, `?? pa/lat ;%%DraftNarrative`).
   *  Coordinates map to their name (including a leading ?/!/~), narratives map to their full text.
   *  If several nodes share an id, the first occurrence in the file wins.
   */
  def fromGenAst(root: GenAst.PCM): Map[String, SourceRange] =
    val acc = mutable.LinkedHashMap.empty[String, SourceRange]
    walk(root.asInstanceOf[js.Dynamic], acc)
    acc.toMap

  private def walk(node: js.Dynamic, acc: mutable.Map[String, SourceRange]): Unit =
    node.selectDynamic("$type").asInstanceOf[String] match
      case "IssueCoordinate" => recordCoordinate(node, "Reference", acc)
      case "OrderCoordinate" => recordCoordinate(node, "Coordinate", acc)
      case "NL_STATEMENT" =>
        val name = node.selectDynamic("name").asInstanceOf[String]
        cstRange(node).foreach { r =>
          acc.getOrElseUpdate(s"$name%%${NarrativeType.fromStatement(name).elkType}", r)
        }
      case _ => ()

    // Langium keeps back-references and the CST under `$`-prefixed keys; cross-references have no `$type`
    js.Object.keys(node.asInstanceOf[js.Object]).foreach { key =>
      if !key.startsWith("$") then
        val value = node.selectDynamic(key)
        if js.Array.isArray(value) then value.asInstanceOf[js.Array[js.Dynamic]].foreach(visit(_, acc))
        else visit(value, acc)
    }

  private def visit(value: js.Dynamic, acc: mutable.Map[String, SourceRange]): Unit =
    val isAstNode =
      js.typeOf(value) == "object" &&
        value.asInstanceOf[js.Any] != null &&
        !js.isUndefined(value.selectDynamic("$type"))
    if isAstNode then walk(value, acc)

  private def recordCoordinate(node: js.Dynamic, suffix: String, acc: mutable.Map[String, SourceRange]): Unit =
    val name = node.selectDynamic("name").asInstanceOf[String]
    nameRange(node, name).foreach(r => acc.getOrElseUpdate(s"$name%%$suffix", r))

  private def nameRange(node: js.Dynamic, name: String): Option[SourceRange] =
    val cst = node.selectDynamic("$cstNode")
    if js.isUndefined(cst) then None
    else
      findLeaf(cst, name).map { nameLeaf =>
        // Start at the first ?/!/~ so the whole "!IC2" is selected, not just "IC2"
        val qu = node.selectDynamic("qu")
        val prefix =
          if js.isUndefined(qu) then None
          else qu.asInstanceOf[js.Array[js.Dynamic]].headOption.flatMap(cstRange)
        prefix match
          case Some(p) => SourceRange(p.startLine, p.startChar, nameLeaf.endLine, nameLeaf.endChar)
          case None    => nameLeaf
      }

  /** First token inside the CST node whose text is exactly `name`. */
  private def findLeaf(cst: js.Dynamic, name: String): Option[SourceRange] =
    val content = cst.selectDynamic("content")
    if js.isUndefined(content) then
      if cst.selectDynamic("text").asInstanceOf[String] == name then Some(toRange(cst.selectDynamic("range")))
      else None
    else
      content
        .asInstanceOf[js.Array[js.Dynamic]]
        .iterator
        .map(findLeaf(_, name))
        .collectFirst { case Some(r) => r }

  private def cstRange(node: js.Dynamic): Option[SourceRange] =
    val cst = node.selectDynamic("$cstNode")
    if js.isUndefined(cst) then None else Some(toRange(cst.selectDynamic("range")))

  private def toRange(r: js.Dynamic): SourceRange =
    SourceRange(
      r.start.line.asInstanceOf[Int],
      r.start.character.asInstanceOf[Int],
      r.end.line.asInstanceOf[Int],
      r.end.character.asInstanceOf[Int]
    )
