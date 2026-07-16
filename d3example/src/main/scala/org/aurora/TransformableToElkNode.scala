package org.aurora

import org.aurora.AuroraElk.Node
import org.aurora.sjsast.*
import typings.elkjs.libElkApiMod.ElkNode
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

trait TransformableToElkNode[A]:
    extension (a: A) def transformToElkNode: ElkNode

given TransformableToElkNode[IssueCoordinate] with
  extension (ic: IssueCoordinate) def transformToElkNode: ElkNode =
    ElkNode(id=ic.name + "%%Reference")

given TransformableToElkNode[OrderCoordinate] with
  extension (oc: OrderCoordinate) def transformToElkNode: ElkNode =
    ElkNode(id=oc.name + "%%Coordinate")

given TransformableToElkNode[NL_STATEMENT] with
  extension (n: NL_STATEMENT) def transformToElkNode: ElkNode =
    ElkNode(id=n.name + "%%Statement")