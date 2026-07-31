package org.aurora.visual.elk

import org.aurora.visual.elk.AuroraElk.Node
import org.aurora.sjsast.*
import org.aurora.sjsast.utils.{NarrativeType, Qualifier}
import typings.elkjs.libElkApiMod.ElkNode
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import typings.elkjs.libElkApiMod.LayoutOptions

trait TransformableToElkNode[A]:
    extension (a: A) def transformToElkNode: ElkNode

given TransformableToElkNode[IssueCoordinate] with
  extension (ic: IssueCoordinate) def transformToElkNode: ElkNode =
    val qualifier = Qualifier.fromQu(ic.qu).elkType
    ElkNode(id=ic.name + "%%Reference").setLayoutOptions(js.Dictionary("aurora.qualifier" -> qualifier).asInstanceOf[LayoutOptions])

given TransformableToElkNode[OrderCoordinate] with
  extension (oc: OrderCoordinate) def transformToElkNode: ElkNode =
    ElkNode(id=oc.name + "%%Coordinate")

given TransformableToElkNode[NL_STATEMENT] with
  extension (n: NL_STATEMENT) def transformToElkNode: ElkNode =
    // 1. Safely resolve the correct enum based on the prefix
    val narrativeType = NarrativeType.fromStatement(n.name)
    
    // 2. Append the strongly typed elkType to the ID
    ElkNode(id = s"${n.name}%%${narrativeType.elkType}")