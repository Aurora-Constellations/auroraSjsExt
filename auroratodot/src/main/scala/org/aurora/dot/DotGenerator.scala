package org.aurora.dot

import org.aurora.sjsast._ 
import org.aurora.sjsast.utils.AstNode.*
import org.aurora.sjsast.utils.{NarrativeType, Qualifier}

object DotGenerator:

  def generate(pcm: PCM): String = 
    val sb = new StringBuilder()
    
    // Graph Header and default styling
    sb.append("digraph AuroraModel {\n")
    sb.append("  rankdir=TB;\n")
    sb.append("  node [shape=box, style=\"rounded,filled\", fillcolor=white, fontname=\"Helvetica\"];\n")
    sb.append("  edge [fontname=\"Helvetica\", color=gray50];\n\n")

    val rootId = "PCM_Root"
    sb.append(s"  $rootId [label=\"PCM Model\", shape=ellipse, fillcolor=lightblue];\n")

    // 1. Traverse Top-Level CIOs via PCM map
    pcm.cio.foreach { case (cioName, cioNode) =>
      val cioId = sanitizeId(s"CIO_$cioName")
      sb.append(s"  $cioId [label=\"${escapeLabel(cioName)}\", fillcolor=lightgrey];\n")
      sb.append(s"  $rootId -> $cioId;\n")

      // 2. Start recursive traversal to maintain parent-child context
      traverseAst(cioNode, cioId, sb)
    }

    sb.append("}\n")
    sb.toString()

  private def traverseAst(node: AstNode, parentId: String, sb: StringBuilder): Unit =
    node match
      case clinical: Clinical =>
        clinical.narratives.foreach(traverseAst(_, parentId, sb))
        clinical.ngc.foreach(traverseAst(_, parentId, sb))
        
      case orders: Orders =>
        orders.narratives.foreach(traverseAst(_, parentId, sb))
        orders.ngo.foreach(traverseAst(_, parentId, sb))
        
      case issues: Issues =>
        issues.narratives.foreach(traverseAst(_, parentId, sb))
        issues.ic.foreach(traverseAst(_, parentId, sb))
        
      case ngc: NGC =>
        val ngcId = sanitizeId(s"NGC_${ngc.name}")
        sb.append(s"  $ngcId [label=\"NGC: ${escapeLabel(ngc.name)}\", fillcolor=lightgreen];\n")
        sb.append(s"  $parentId -> $ngcId;\n")
        
        ngc.narratives.foreach(traverseAst(_, ngcId, sb))
        ngc.coordinates.foreach(traverseAst(_, ngcId, sb))
        
      case ngo: NGO =>
        val ngoId = sanitizeId(s"NGO_${ngo.name}")
        sb.append(s"  $ngoId [label=\"NGO: ${escapeLabel(ngo.name)}\", fillcolor=lightpink];\n")
        sb.append(s"  $parentId -> $ngoId;\n")
        
        ngo.narratives.foreach(traverseAst(_, ngoId, sb))
        ngo.ordercoord.foreach(traverseAst(_, ngoId, sb))

      case ic: IssueCoordinate =>
        val coordId = getCoordId(ic.name)
        val qual = getQualifier(ic)
        
        // Compute node border attributes based on Qualifier (!, ~, ?)
        val nodeAttrs = getNodeBorderAttributes(qual, "lightcoral")
        
        sb.append(s"  $coordId [label=\"${escapeLabel(ic.name)}\", shape=ellipse$nodeAttrs];\n")
        // Standard normal parent-to-child edge (no qualifier styling on edge anymore)
        sb.append(s"  $parentId -> $coordId;\n") 
        
        ic.narratives.foreach(traverseAst(_, coordId, sb))
        processCrossReferences(ic.qurefs, coordId, sb)

      case coord: ClinicalItem =>
        val coordId = getCoordId(coord.name)
        val qual = getQualifier(coord)
        val nodeAttrs = getNodeBorderAttributes(qual, "lightyellow")
        
        sb.append(s"  $coordId [label=\"${escapeLabel(coord.name)}\", shape=ellipse$nodeAttrs];\n")
        sb.append(s"  $parentId -> $coordId;\n") 
        
        coord.narratives.foreach(traverseAst(_, coordId, sb))
        processCrossReferences(coord.qurefs, coordId, sb)

      case oCoord: OrderCoordinate =>
        val coordId = getCoordId(oCoord.name)
        val qual = getQualifier(oCoord)
        val nodeAttrs = getNodeBorderAttributes(qual, "lightyellow")
        
        sb.append(s"  $coordId [label=\"${escapeLabel(oCoord.name)}\", shape=ellipse$nodeAttrs];\n")
        sb.append(s"  $parentId -> $coordId;\n") 
        
        oCoord.narratives.foreach(traverseAst(_, coordId, sb))
        processCrossReferences(oCoord.qurefs, coordId, sb)

      case nar: NL_STATEMENT =>
        val narId = sanitizeId(s"NL_${nar.name}_${nar.hashCode}")
        val nType = NarrativeType.fromStatement(nar.name)
        val fillColor = nType match
          case NarrativeType.Draft | NarrativeType.DraftCompleted   => "yellow"
          case NarrativeType.Urgent | NarrativeType.UrgentCompleted => "red"
          case _                                                    => "cornsilk"
        
        sb.append(s"  $narId [label=\"${escapeLabel(nar.name)}\", shape=note, fillcolor=$fillColor];\n")
        sb.append(s"  $parentId -> $narId [style=dashed, color=orange];\n") 

      case _ => 
        // Safely ignore unknown AstNode types

  // Maps the Qualifier to node-level border styling properties
  private def getNodeBorderAttributes(qualifier: Qualifier, defaultFill: String): String =
    qualifier match
      case Qualifier.Urgent =>
        // ! -> Solid red border
        s", fillcolor=$defaultFill, color=red, penwidth=2.0, style=\"rounded,filled\""
      case Qualifier.Negative =>
        // ~ -> Dashed red border
        s", fillcolor=$defaultFill, color=red, penwidth=2.0, style=\"rounded,filled,dashed\""
      case Qualifier.Draft =>
        // ? -> Yellow/gold solid border
        s", fillcolor=$defaultFill, color=gold, penwidth=2.0, style=\"rounded,filled\""
      case _ =>
        // Normal -> Default fill, standard border (IC1 case)
        s", fillcolor=$defaultFill, style=\"rounded,filled\""

  private def processCrossReferences(qurefs: Iterable[QuReferences], sourceId: String, sb: StringBuilder): Unit =
    if (qurefs != null) {
      qurefs.foreach { refsNode =>
        if (refsNode.qurc != null) {
          refsNode.qurc.foreach { qRef =>
            val targetId = getCoordId(qRef.refName)
            sb.append(s"  $sourceId -> $targetId [constraint=false];\n")
          }
        }
      }
    }

  private def getCoordId(name: String): String =
    sanitizeId(s"Coord_$name")

  private def sanitizeId(str: String): String =
    str.replaceAll("[^a-zA-Z0-9_]", "_")

  private def escapeLabel(str: String): String =
    str.replace("\"", "\\\"").replace("\n", "\\n")