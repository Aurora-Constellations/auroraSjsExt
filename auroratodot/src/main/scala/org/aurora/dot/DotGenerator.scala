package org.aurora.dot

import org.aurora.sjsast._ 

object DotGenerator:

  /**
   * Main entry point to convert a PCM object into a DOT formatted string.
   */
  def generate(pcm: PCM): String = 
    val sb = new StringBuilder()
    
    // Graph Header and default styling
    sb.append("digraph AuroraModel {\n")
    sb.append("  rankdir=TB;\n") // Top to Bottom layout
    sb.append("  node [shape=box, style=\"rounded,filled\", fillcolor=white, fontname=\"Helvetica\"];\n")
    sb.append("  edge [fontname=\"Helvetica\", color=gray50];\n\n")

    // Root Node
    val rootId = "PCM_Root"
    sb.append(s"  $rootId [label=\"PCM Model\", shape=ellipse, fillcolor=lightblue];\n")

    // Traverse the CIO Map
    pcm.cio.foreach { case (cioName, cioNode) =>
      val cioId = sanitizeId(s"CIO_$cioName")
      sb.append(s"  $cioId [label=\"${escapeLabel(cioName)}\", fillcolor=lightgrey];\n")
      sb.append(s"  $rootId -> $cioId;\n")

      // Pattern match on the specific CIO types to dig deeper
      cioNode match
        case clinical: Clinical =>
          clinical.ngc.foreach { ngc =>
            val ngcId = sanitizeId(s"NGC_${ngc.name}")
            sb.append(s"  $ngcId [label=\"NGC: ${escapeLabel(ngc.name)}\", fillcolor=lightgreen];\n")
            sb.append(s"  $cioId -> $ngcId;\n")
            
            // Map Narratives (NL_STATEMENT)
            processNarratives(ngc.narratives, ngcId, sb)
            
            // Map Coordinates (ClinicalItem) - assuming they have a 'name' property
            ngc.coordinates.foreach { coord =>
              // Using hashCode to ensure unique IDs if names overlap
              val coordId = sanitizeId(s"Coord_${coord.hashCode}") 
              sb.append(s"  $coordId [label=\"Item\", shape=ellipse, fillcolor=lightyellow];\n") // Update label based on actual ClinicalItem properties
              sb.append(s"  $ngcId -> $coordId;\n")
            }
          }

        case orders: Orders =>
          orders.ngo.foreach { ngo =>
            val ngoId = sanitizeId(s"NGO_${ngo.name}")
            sb.append(s"  $ngoId [label=\"NGO: ${escapeLabel(ngo.name)}\", fillcolor=lightpink];\n")
            sb.append(s"  $cioId -> $ngoId;\n")

            // Map Narratives
            processNarratives(ngo.narratives, ngoId, sb)
            
            // Map OrderCoordinates
            ngo.ordercoord.foreach { oCoord =>
              val coordId = sanitizeId(s"OCoord_${oCoord.hashCode}")
              sb.append(s"  $coordId [label=\"OrderCoord\", shape=ellipse, fillcolor=lightyellow];\n") // Update label based on actual OrderCoordinate properties
              sb.append(s"  $ngoId -> $coordId;\n")
            }
          }

        case issues: Issues =>
          // You can expand this based on the Issues AST structure
          val issuesNoteId = sanitizeId(s"Note_${issues.name}")
          sb.append(s"  $issuesNoteId [label=\"Issues handling coming soon\", shape=note];\n")
          sb.append(s"  $cioId -> $issuesNoteId;\n")
    }

    // Close Graph
    sb.append("}\n")
    sb.toString()

  /**
   * Helper to process narratives for both NGC and NGO uniformly
   */
  private def processNarratives(narratives: Iterable[NL_STATEMENT], parentId: String, sb: StringBuilder): Unit =
    narratives.foreach { nar =>
      val narId = sanitizeId(s"NL_${nar.name}_${nar.hashCode}")
      sb.append(s"  $narId [label=\"${escapeLabel(nar.name)}\", shape=note, fillcolor=cornsilk];\n")
      sb.append(s"  $parentId -> $narId [style=dashed];\n") // Dashed line for narratives
    }

  /**
   * Helper to ensure valid Graphviz IDs (alphanumeric and underscores only)
   */
  private def sanitizeId(str: String): String =
    str.replaceAll("[^a-zA-Z0-9_]", "_")

  /**
   * Helper to escape strings so they don't break the DOT file syntax
   */
  private def escapeLabel(str: String): String =
    str.replace("\"", "\\\"").replace("\n", "\\n")