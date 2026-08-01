package org.aurora

import org.aurora.visual.elk.AuroraElk

trait Show[A]:
  def show(a: A): String

object Show:
  extension [A](x: A)(using s: Show[A])
    def show: String = s.show(x)

  given Show[AuroraElk.Node] with 
    def show(node: AuroraElk.Node): String = s"Node(id=${node.id}, children=${node.children.size})" 
