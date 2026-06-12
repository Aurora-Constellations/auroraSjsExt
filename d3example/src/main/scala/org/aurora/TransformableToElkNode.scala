package org.aurora

import org.aurora.AuroraElk.AuroraElkNode
import org.aurora.sjsast.*
import typings.elkjs.libElkApiMod.ElkNode
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

trait TransformableToElkNode[A]:
    extension (a: A) def transformToElkNode: ElkNode

given TransformableToElkNode[IssueCoordinate] with
  extension (ic: IssueCoordinate) def transformToElkNode: ElkNode =
    js.Dynamic.literal(id = ic.name, children = ???).asInstanceOf[ElkNode]

given TransformableToElkNode[OrderCoordinate] with
  extension (oc: OrderCoordinate) def transformToElkNode: ElkNode =
    js.Dynamic.literal(id = oc.name, children = ???).asInstanceOf[ElkNode]

given TransformableToElkNode[NL_STATEMENT] with
  extension (n: NL_STATEMENT) def transformToElkNode: ElkNode =
    js.Dynamic.literal(id = n.name, children = js.Array()).asInstanceOf[ElkNode]


