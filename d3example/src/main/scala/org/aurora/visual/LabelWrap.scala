package org.aurora.visual

// Wraps long node labels (e.g. narratives) onto several lines. Used both to size nodes for the ELK layout and to
// draw their text, so a node's box always fits its lines.
object LabelWrap:
    // Longest line, in characters, before the label wraps
    val MaxLineChars = 32
    // Rough width of one character of the 12px semi-bold node label
    val CharWidth = 7.0
    val HorizontalPadding = 20.0
    // Height of a one-line node, and what each extra line adds
    val SingleLineHeight = 40.0
    val LineHeight = 16.0

    // Breaks at spaces, or after an underscore inside a long name (e.g. "review_documented_stroke_risk_factors");
    // a piece still longer than a whole line is split
    def lines(text: String, maxChars: Int = MaxLineChars): List[String] = {
        // (piece, starts a new word): pieces of the same word are joined without a space
        val pieces = text.trim.split("\\s+").toList.filter(_.nonEmpty).flatMap { word =>
            // "a_b_c" -> "a_", "b_", "c" (each piece keeps its trailing underscore)
            val underscoreParts = word.split("_", -1).toList
            val parts = underscoreParts.zipWithIndex
                .map { case (p, i) => if (i < underscoreParts.size - 1) p + "_" else p }
                .filter(_.nonEmpty)
                .flatMap(_.grouped(maxChars))
            parts.zipWithIndex.map { case (part, i) => (part, i == 0) }
        }
        val wrapped = pieces.foldLeft(List.empty[String]) {
            case (current :: done, (piece, newWord)) =>
                val joined = if (newWord) s"$current $piece" else current + piece
                if (joined.length <= maxChars) joined :: done else piece :: current :: done
            case (Nil, (piece, _)) => List(piece)
        }
        if (wrapped.isEmpty) List(text.trim) else wrapped.reverse
    }

    def width(lines: List[String]): Double =
        lines.map(_.length).maxOption.getOrElse(0) * CharWidth + HorizontalPadding

    def height(lines: List[String]): Double =
        SingleLineHeight + math.max(0, lines.size - 1) * LineHeight
