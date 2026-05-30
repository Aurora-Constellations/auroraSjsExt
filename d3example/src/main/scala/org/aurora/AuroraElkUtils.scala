package org.aurora

object AuroraElkUtils {  

  enum Layout:
    case LAYERED, FORCE, RADIAL

  enum Direction:
    case UP, DOWN

  final case class AuroraElkParameters(
    layout: Layout,
    direction: Direction
  )
}
