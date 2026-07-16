package org.aurora

import org.aurora.AuroraElk.Node
import org.aurora.sjsast.*
import typings.elkjs.libElkApiMod.ElkNode
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import org.aurora.AuroraElkUtils.getDrawableDescendants

trait TransformableToElkNode[A]:
    extension (a: A) def transformToElkNode: ElkNode

given TransformableToElkNode[IssueCoordinate] with
  extension (ic: IssueCoordinate) def transformToElkNode: ElkNode =
    ElkNode(id=ic.name + "%%Reference")
      .setChildren(getDrawableDescendants(ic).toJSArray)

given TransformableToElkNode[OrderCoordinate] with
  extension (oc: OrderCoordinate) def transformToElkNode: ElkNode =
    ElkNode(id=oc.name + "%%Coordinate")
      .setChildren(getDrawableDescendants(oc).toJSArray)

given TransformableToElkNode[NL_STATEMENT] with
  extension (n: NL_STATEMENT) def transformToElkNode: ElkNode =
    ElkNode(id=n.name + "%%Statement")
      .setChildren(getDrawableDescendants(n).toJSArray)