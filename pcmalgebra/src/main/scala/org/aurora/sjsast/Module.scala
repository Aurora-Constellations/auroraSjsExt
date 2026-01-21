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
    val cioMap = cioFromElements(m.elements.asInstanceOf[js.Array[Any]])
    new Module(m.name, cioMap)

  def apply(pcm: G.PCM): Module =
    val dyn = pcm.asInstanceOf[js.Dynamic]
    
    if !js.isUndefined(dyn.module) then
       apply(dyn.module.asInstanceOf[G.Module])
    else
       val elems = if !js.isUndefined(dyn.elements) then dyn.elements.asInstanceOf[js.Array[Any]] else js.Array()
       val cioMap = cioFromElements(elems)
       new Module("root", cioMap)

  private def getType(node: Any): String =
    if node != null then
      val dyn = node.asInstanceOf[js.Dynamic]
      if !js.isUndefined(dyn.selectDynamic("$type")) then
        dyn.selectDynamic("$type").toString
      else ""
    else ""

  private def extractQuSet(quArray: js.Array[G.QU]): LHSet[QU] =
    LinkedHashSet.from(quArray.toSeq.map(QU.fromJs))

  private def extractQU(quArray: js.Array[G.QU]): QU =
    QU.fromJsArray(quArray)

  private def extractQuRefs(qurc: js.UndefOr[G.QuReferences]): QuReferences =
    qurc.toOption match {
      case Some(refsObj) =>
        QuReferences.fromJs(refsObj)
      case None => QuReferences()
    }

  private def cioFromElements(elements: js.Array[Any]): LinkedHashMap[String, CIO] =
    val map = LinkedHashMap.empty[String, CIO]

    elements.foreach { element =>
      getType(element) match
        case "Clinical" =>
           val c = element.asInstanceOf[G.Clinical]
           val groups = c.namedGroups.map { ng =>
             val coords = ng.coord.flatMap { item =>
               if getType(item) == "ClinicalCoordinate" then
                 val cc = item.asInstanceOf[G.ClinicalCoordinate]
                 Some(ClinicalCoordinate(
                   name = cc.name, 
                   narratives = NL_STATEMENT.fromJsSeq(cc.narrative.toSeq),
                   refs = extractQuRefs(cc.qurc),
                   qu = extractQU(cc.qu)
                 ))
               else None
             }
             
             NGC(
               name = ng.name, 
               narratives = NL_STATEMENT.fromJsSeq(ng.narrative.toSeq),
               coordinates = LinkedHashSet.from(coords),
               refs = extractQuRefs(ng.asInstanceOf[js.Dynamic].qurc.asInstanceOf[js.UndefOr[G.QuReferences]])
             )
           }

           val section = Clinical(
             narratives = NL_STATEMENT.fromJsSeq(c.narrative.toSeq),
             namedGroups = LinkedHashSet.from(groups)
           )
           map.update("Clinical", section)

        case "Issues" =>
          val i = element.asInstanceOf[G.Issues]
          val coords = i.coord.map { ic =>
             IssueCoordinate(
               name = ic.name,
               narratives = NL_STATEMENT.fromJsSeq(ic.narrative.toSeq),
               refs = extractQuRefs(ic.qurc),
               qu = extractQU(ic.qu)
             )
          }
          
          val section = Issues(
            narratives = NL_STATEMENT.fromJsSeq(i.narrative.toSeq),
            coordinates = LinkedHashSet.from(coords)
          )
          map.update("Issues", section)

        case "Orders" =>
            val o = element.asInstanceOf[G.Orders]
            val groups = o.namedGroups.map { ng =>
              val orderItems = ng.orders.flatMap { item =>
                if getType(item) == "OrderCoordinate" then
                  val oc = item.asInstanceOf[G.OrderCoordinate]
                  Some(OrderCoordinate(
                    name = oc.name,
                    narratives = NL_STATEMENT.fromJsSeq(oc.narrative.toSeq),
                    refs = extractQuRefs(oc.qurc)
                  ))
                else None
              }
              
              NGO(
                name = ng.name,
                narratives = NL_STATEMENT.fromJsSeq(ng.narrative.toSeq),
                orders = LinkedHashSet.from(orderItems),
                refs = extractQuRefs(ng.asInstanceOf[js.Dynamic].qurc.asInstanceOf[js.UndefOr[G.QuReferences]]),
                qu = extractQuSet(ng.asInstanceOf[js.Dynamic].qu.asInstanceOf[js.Array[G.QU]])
              )
            }

            val section = Orders(
              narratives = NL_STATEMENT.fromJsSeq(o.narrative.toSeq),
              namedGroups = LinkedHashSet.from(groups)
            )
            map.update("Orders", section)
        case _ => // Ignore
      }
    map