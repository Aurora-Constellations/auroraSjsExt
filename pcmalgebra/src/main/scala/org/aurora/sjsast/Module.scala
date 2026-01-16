package org.aurora.sjsast

import scala.collection.mutable.{LinkedHashMap, LinkedHashSet}
import org.aurora.sjsast.{GenAst => G}
import scala.scalajs.js


case class Module(
    name: String,
    cio: LinkedHashMap[String, CIO] = LinkedHashMap.empty
)

object Module:
  def apply(m: G.Module): Module =
    val cioMap = cioFromModuleElements(m)
    new Module(m.name, cioMap)

  // Helper: Access the "$type" property dynamically to verify the node type
  private def getType(node: Any): String =
    if node != null then
      val dyn = node.asInstanceOf[js.Dynamic]
      if !js.isUndefined(dyn.selectDynamic("$type")) then
        dyn.selectDynamic("$type").toString
      else ""
    else ""

  private def cioFromModuleElements(m: G.Module): LinkedHashMap[String, CIO] =
    val map = LinkedHashMap.empty[String, CIO]

    // Use getType(element) instead of standard pattern matching
    m.elements.foreach { element =>
      getType(element) match
        // --- Handle Clinical ---
        case "Clinical" =>
           val c = element.asInstanceOf[G.Clinical]
           val groups = c.namedGroups.map { ng =>
             // Filter Mixed Coordinates manually
             val coords = ng.coord.flatMap { item =>
               if getType(item) == "ClinicalCoordinate" then
                 val cc = item.asInstanceOf[G.ClinicalCoordinate]
                 Some(ClinicalCoordinate(cc.name, NL_STATEMENT.fromJsSeq(cc.narrative.toSeq)))
               else None
             }
             
             NGC(
               name = ng.name, 
               narratives = NL_STATEMENT.fromJsSeq(ng.narrative.toSeq),
               coordinates = LinkedHashSet.from(coords)
             )
           }

           val section = Clinical(
             narratives = NL_STATEMENT.fromJsSeq(c.narrative.toSeq),
             namedGroups = LinkedHashSet.from(groups)
           )
           map.update("clinical", section)

        // --- Handle Issues ---
        case "Issues" =>
          val i = element.asInstanceOf[G.Issues]
          // i.coord usually strictly contains IssueCoordinate, but safe to map directly if typed
          val coords = i.coord.map { ic =>
             IssueCoordinate(
               name = ic.name,
               narratives = NL_STATEMENT.fromJsSeq(ic.narrative.toSeq)
             )
          }
          
          val section = Issues(
            narratives = NL_STATEMENT.fromJsSeq(i.narrative.toSeq),
            coordinates = LinkedHashSet.from(coords)
          )
          map.update("issues", section)

        // --- Handle Orders ---
        case "Orders" =>
          val o = element.asInstanceOf[G.Orders]
          val groups = o.namedGroups.map { ng =>
             // ng.orders contains union of OrderCoordinate | MutuallyExclusive
             val orderItems = ng.orders.flatMap { item =>
               if getType(item) == "OrderCoordinate" then
                 val oc = item.asInstanceOf[G.OrderCoordinate]
                 Some(OrderCoordinate(
                   name = oc.name, 
                   narratives = NL_STATEMENT.fromJsSeq(oc.narrative.toSeq)
                 ))
               else None
             }
             
             NGO(
               name = ng.name,
               narratives = NL_STATEMENT.fromJsSeq(ng.narrative.toSeq),
               orders = LinkedHashSet.from(orderItems)
             )
          }

          val section = Orders(
            narratives = NL_STATEMENT.fromJsSeq(o.narrative.toSeq),
            namedGroups = LinkedHashSet.from(groups)
           )
           map.update("orders", section)

        case _ => // Ignore other nodes
      }
    map