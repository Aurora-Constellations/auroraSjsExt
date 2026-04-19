package org.aurora
import org.aurora.sjsast.*
import org.aurora.ElkBuilders.*
import typings.elkjs.libElkApiMod.{ElkNode, ElkEdge}

object AstToElk {

  /* can be customized later based on node type */
  private val width  = 150.0
  private val height = 50.0

  def toElkRoot(ast: Any): ElkNode = {
    val topChildren: Seq[ElkNode] = ast match {
      case pcm: PCM =>
        pcm.cio.values.toSeq.flatMap(toTopNodes)

      case orders: Orders =>
        orders.ngo.toSeq.flatMap(toTopNodes)

      case other =>
        toTopNodes(other)
    }

    node(
      id = "root",
      width = 10,
      height = 10,
      children = topChildren,
      edges = Seq.empty,
      layoutOptions = Map(
        "elk.algorithm" -> "layered",
        "elk.direction" -> "DOWN",
        "elk.spacing.nodeNode" -> "40",
        "elk.layered.spacing.nodeNodeBetweenLayers" -> "60"
      )
    )
  }

  private def toTopNodes(ast: Any): Seq[ElkNode] =
    ast match {
      case ngo: NGO              => Seq(toElkNode(ngo))
      case oc: OrderCoordinate   => Seq(toElkNode(oc))
      case nar: NL_STATEMENT     => Seq(toElkNode(nar))
      case orders: Orders        => orders.ngo.toSeq.map(toElkNode)
      case pcm: PCM              => pcm.cio.values.toSeq.flatMap(toTopNodes)
      case _                     => Seq.empty
    }

  private def toElkNode(ngo: NGO): ElkNode = {
    val children: Seq[ElkNode] =
      ngo.ordercoord.toSeq.map(toElkNode)

    val edges: Seq[ElkEdge] =
      ngo.ordercoord.toSeq.map { oc =>
        edge(
          id = s"${safeId(ngo.name)}__${safeId(oc.name)}",
          source = ngo.name,
          target = oc.name
        )
      }

    node(
      id = ngo.name,
      width = width,
      height = height,
      children = children,
      edges = Seq.empty
    )
  }

  private def toElkNode(oc: OrderCoordinate): ElkNode = {
    val children: Seq[ElkNode] =
      oc.narratives.toSeq.map(toElkNode)

    val edges: Seq[ElkEdge] =
      oc.narratives.toSeq.map { nar =>
        edge(
          id = s"${safeId(oc.name)}__${safeId(nar.name)}",
          source = oc.name,
          target = nar.name
        )
      }

    node(
      id = oc.name,
      width = width,
      height = height,
      children = children,
      edges = Seq.empty
    )
  }

  private def toElkNode(nar: NL_STATEMENT): ElkNode =
    node(
      id = nar.name,
      width = width,
      height = height,
      children = Seq.empty,
      edges = Seq.empty
    )

  private def safeId(s: String): String =
    s.replaceAll("\\s+", "_")
}

