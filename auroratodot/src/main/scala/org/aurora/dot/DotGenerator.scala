package org.aurora.dot

import org.aurora.sjsast._ 
import org.aurora.sjsast.utils.AstNode.*
import org.aurora.sjsast.utils.{NarrativeType, Qualifier}

object DotGenerator:

  def generate(pcm: PCM): String = 
    given sb: StringBuilder = new StringBuilder()
    
    // Graph Header and default styling
    append("digraph AuroraModel {")
    append("  rankdir=TB;")
    append("  node [shape=box, style=\"rounded,filled\", fillcolor=white, fontname=\"Helvetica\"];")
    append("  edge [fontname=\"Helvetica\", color=gray50];\n")

    val rootId = "PCM_Root"
    appendNode(rootId, "PCM Model", "ellipse", "lightblue")

    // 1. Traverse Top-Level CIOs
    pcm.cio.foreach { case (cioName, cioNode) =>
      val cioId = sanitizeId(s"CIO_$cioName")
      appendNode(cioId, cioName, "box", "lightgrey")
      appendEdge(rootId, cioId)

      // 2. Start recursive traversal passing the implicit StringBuilder
      traverseAst(cioNode, cioId)
    }

    append("}")
    sb.toString()

  // --- 1. AST Traversal Engine ---

  private def traverseAst(node: AstNode, parentId: String)(using sb: StringBuilder): Unit =
    node match
      case clinical: Clinical =>
        clinical.narratives.foreach(traverseAst(_, parentId))
        clinical.ngc.foreach(traverseAst(_, parentId))
        
      case orders: Orders =>
        orders.narratives.foreach(traverseAst(_, parentId))
        orders.ngo.foreach(traverseAst(_, parentId))
        
      case issues: Issues =>
        issues.narratives.foreach(traverseAst(_, parentId))
        issues.ic.foreach(traverseAst(_, parentId))
        
      case ngc: NGC =>
        val ngcId = sanitizeId(s"NGC_${ngc.name}")
        appendNode(ngcId, s"NGC: ${ngc.name}", "box", "lightgreen")
        appendEdge(parentId, ngcId)
        
        ngc.narratives.foreach(traverseAst(_, ngcId))
        ngc.coordinates.foreach(traverseAst(_, ngcId))
        
      case ngo: NGO =>
        val ngoId = sanitizeId(s"NGO_${ngo.name}")
        appendNode(ngoId, s"NGO: ${ngo.name}", "box", "lightpink")
        appendEdge(parentId, ngoId)
        
        ngo.narratives.foreach(traverseAst(_, ngoId))
        ngo.ordercoord.foreach(traverseAst(_, ngoId))

      case ic: IssueCoordinate =>
        val coordId = getCoordId(ic.name)
        val qual = getQualifier(ic)
        
        appendNode(coordId, ic.name, "ellipse", "lightcoral", getBorderAttrs(qual))
        appendEdge(parentId, coordId, getEdgeAttrs(qual, isCrossReference = false))
        
        ic.narratives.foreach(traverseAst(_, coordId))
        processCrossReferences(ic.qurefs, coordId)

      case coord: ClinicalItem =>
        val coordId = getCoordId(coord.name)
        val qual = getQualifier(coord)
        
        appendNode(coordId, coord.name, "ellipse", "lightyellow", getBorderAttrs(qual))
        appendEdge(parentId, coordId, getEdgeAttrs(qual, isCrossReference = false))
        
        coord.narratives.foreach(traverseAst(_, coordId))
        processCrossReferences(coord.qurefs, coordId)

      case oCoord: OrderCoordinate =>
        val coordId = getCoordId(oCoord.name)
        val qual = getQualifier(oCoord)
        
        appendNode(coordId, oCoord.name, "ellipse", "lightyellow", getBorderAttrs(qual))
        appendEdge(parentId, coordId, getEdgeAttrs(qual, isCrossReference = false))
        
        oCoord.narratives.foreach(traverseAst(_, coordId))
        processCrossReferences(oCoord.qurefs, coordId)

      case nar: NL_STATEMENT =>
        val narId = sanitizeId(s"NL_${nar.name}_${nar.hashCode}")
        val nType = NarrativeType.fromStatement(nar.name)
        val fillColor = nType match
          case NarrativeType.Draft | NarrativeType.DraftCompleted   => "yellow"
          case NarrativeType.Urgent                                 => "red"
          case NarrativeType.UrgentCompleted                        => "purple"
          case _                                                    => "cornsilk"
        
        appendNode(narId, nar.name, "note", fillColor)
        appendEdge(parentId, narId)

      case _ => 
        // Safely ignore unhandled nodes

  // --- 2. Cross-Reference Mapper ---

  private def processCrossReferences(qurefs: Iterable[QuReferences], sourceId: String)(using sb: StringBuilder): Unit =
    if qurefs != null then
      qurefs.foreach { refsNode =>
        if refsNode.qurc != null then
          refsNode.qurc.foreach { qRef =>
            val targetId = getCoordId(qRef.refName)
            val qual = Qualifier.fromQu(qRef.qu) // Extract qualifier specific to the reference
            
            appendEdge(sourceId, targetId, getEdgeAttrs(qual, isCrossReference = true))
          }
      }

  // --- 3. Semantic Styling Engine ---

  private def getBorderAttrs(qual: Qualifier): String =
    qual match
      case Qualifier.Urgent   => "color=red, penwidth=2.0"
      case Qualifier.Negative => "color=red, penwidth=2.0, style=\"rounded,filled,dashed\""
      case Qualifier.Draft    => "color=gold, penwidth=2.0"
      case _                  => ""

  private def getEdgeAttrs(qual: Qualifier, isCrossReference: Boolean): String =
    val baseStyle = qual match
      case Qualifier.Urgent   => "color=red, style=solid, penwidth=1.5"   // !
      case Qualifier.Negative => "color=red, style=dashed, penwidth=1.5"  // ~
      case Qualifier.Draft    => "color=gold, style=solid, penwidth=1.5"  // ?
      case _                  => ""                                       // Normal
    
    // Prevent layout distortion for cross-reference connections
    val constraint = if isCrossReference then "constraint=false" else ""
    List(baseStyle, constraint).filter(_.nonEmpty).mkString(", ")

  // --- 4. DOT Sub-Builders ---

  private def append(str: String)(using sb: StringBuilder): Unit =
    sb.append(str).append("\n")

  private def appendNode(id: String, label: String, shape: String, fillcolor: String, extraAttrs: String = "")(using sb: StringBuilder): Unit =
    val attrs = if extraAttrs.isEmpty then "" else s", $extraAttrs"
    append(s"  $id [label=\"${escapeLabel(label)}\", shape=$shape, fillcolor=$fillcolor$attrs];")

  private def appendEdge(from: String, to: String, attrs: String = "")(using sb: StringBuilder): Unit =
    val attrStr = if attrs.isEmpty then "" else s" [$attrs]"
    append(s"  $from -> $to$attrStr;")

  private def getCoordId(name: String): String = sanitizeId(s"Coord_$name")
  private def sanitizeId(str: String): String = str.replaceAll("[^a-zA-Z0-9_]", "_")
  private def escapeLabel(str: String): String = str.replace("\"", "\\\"").replace("\n", "\\n")