package org.aurora.dot

import org.aurora.sjsast._ 
import org.aurora.sjsast.utils.AstNode.*
import org.aurora.sjsast.utils.{NarrativeType, Qualifier}

object DotGenerator:

  // --- 1. State Management Context ---
  // Segregates edges and nodes into separate buffers during a single traversal pass
  private class DotContext:
    val edges = new StringBuilder()
    val nodes = new StringBuilder()

  def generate(pcm: PCM): String = 
    given ctx: DotContext = new DotContext()
    
    val rootId = "PCM_Root"
    appendNode(rootId, "PCM Model", "ellipse", "lightblue")

    // 1. Traverse Top-Level CIOs
    pcm.cio.foreach { case (cioName, cioNode) =>
      val cioId = sanitizeId(s"CIO_$cioName")
      appendNode(cioId, cioName, "box", "lightgrey")
      appendEdge(rootId, cioId)

      // 2. Start recursive traversal passing the implicit context
      traverseAst(cioNode, cioId)
    }

    // 3. Assemble the final DOT file using the requested skeleton
    val finalSb = new StringBuilder()
    
    finalSb.append("digraph AuroraModel {\n")
    finalSb.append("  // Attributes at top level apply to the graph itself.\n")
    finalSb.append("  rankdir=TB;\n")
    finalSb.append("  outputorder=edgesfirst;\n")
    finalSb.append("  pad=\"0.25\";\n")
    finalSb.append("  layout=dot;\n\n")
    
    finalSb.append("  // Default node attributes\n")
    finalSb.append("  node [\n")
    finalSb.append("    shape = circle\n")
    finalSb.append("    style = \"filled\"\n")
    finalSb.append("    color = black\n")
    finalSb.append("    fillcolor = \"#F2F2F2\"\n")
    finalSb.append("    fontname = \"Helvetica\"\n")
    finalSb.append("  ];\n\n")

    finalSb.append("  // Default edge attributes\n")
    finalSb.append("  edge [fontname=\"Helvetica\", color=gray50];\n\n")

    finalSb.append("  // --- Edges ---\n")
    finalSb.append(ctx.edges.toString())
    
    finalSb.append("\n  // --- Node attributes ---\n")
    finalSb.append(ctx.nodes.toString())
    
    finalSb.append("}\n")
    finalSb.toString()

  // --- 2. AST Traversal Engine ---

  private def traverseAst(node: AstNode, parentId: String)(using ctx: DotContext): Unit =
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

  // --- 3. Cross-Reference Mapper ---

  private def processCrossReferences(qurefs: Iterable[QuReferences], sourceId: String)(using ctx: DotContext): Unit =
    if qurefs != null then
      qurefs.foreach { refsNode =>
        if refsNode.qurc != null then
          refsNode.qurc.foreach { qRef =>
            val targetId = getCoordId(qRef.refName)
            val qual = Qualifier.fromQu(qRef.qu) // Extract qualifier specific to the reference
            
            appendEdge(sourceId, targetId, getEdgeAttrs(qual, isCrossReference = true))
          }
      }

  // --- 4. Semantic Styling Engine ---

  private def getBorderAttrs(qual: Qualifier): String =
    qual match
      case Qualifier.Urgent   => "color=red, penwidth=2.0"
      case Qualifier.Negative => "color=red, penwidth=2.0, style=\"filled,bold,dashed\"" // Updated style to respect top-level defaults
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

  // --- 5. DOT Sub-Builders ---

  private def appendNode(id: String, label: String, shape: String, fillcolor: String, extraAttrs: String = "")(using ctx: DotContext): Unit =
    val attrs = if extraAttrs.isEmpty then "" else s", $extraAttrs"
    // Using quotes around fillcolor ensures hex codes (e.g. "#FBB5AE") are handled safely
    ctx.nodes.append(s"  $id [label=\"${escapeLabel(label)}\", shape=$shape, fillcolor=\"$fillcolor\"$attrs];\n")

  private def appendEdge(from: String, to: String, attrs: String = "")(using ctx: DotContext): Unit =
    val attrStr = if attrs.isEmpty then "" else s" [$attrs]"
    ctx.edges.append(s"  $from -> $to$attrStr;\n")

  private def getCoordId(name: String): String = sanitizeId(s"Coord_$name")
  private def sanitizeId(str: String): String = str.replaceAll("[^a-zA-Z0-9_]", "_")
  private def escapeLabel(str: String): String = str.replace("\"", "\\\"").replace("\n", "\\n")