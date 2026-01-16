package org.aurora.sjsast

import scala.collection.mutable.{LinkedHashMap, LinkedHashSet}
import org.aurora.sjsast.{GenAst => G}
import scala.scalajs.js

case class ModulePCM(
    name: String,
    // CIO is now the sealed trait
    cio: LinkedHashMap[String, CIO] = LinkedHashMap.empty
)

object ModulePCM:
  def apply(m: G.Module): ModulePCM =
    val cioMap = cioFromElements(m.elements.asInstanceOf[js.Array[Any]])
    new ModulePCM(m.name, cioMap)

  def apply(pcm: G.PCM): ModulePCM =
    val dyn = pcm.asInstanceOf[js.Dynamic]
    
    if !js.isUndefined(dyn.module) then
       apply(dyn.module.asInstanceOf[G.Module])
    else
       val elems = if !js.isUndefined(dyn.elements) then dyn.elements.asInstanceOf[js.Array[Any]] else js.Array()
       val cioMap = cioFromElements(elems)
       new ModulePCM("root", cioMap)

  private def getType(node: Any): String =
    if node != null then
      val dyn = node.asInstanceOf[js.Dynamic]
      if !js.isUndefined(dyn.selectDynamic("$type")) then
        dyn.selectDynamic("$type").toString
      else ""
    else ""

  private def extractQuSet(quArray: js.Array[G.QU]): LHSet[QU] =
    val set = LinkedHashSet.empty[QU]
    if (quArray != null) {
      quArray.foreach { q =>
        val d = q.asInstanceOf[js.Dynamic]
        val sym = if (js.typeOf(q) == "string") q.toString
                  else if (!js.isUndefined(d.name)) d.name.toString
                  else if (!js.isUndefined(d.value)) d.value.toString
                  else ""
        if (sym.nonEmpty) set.add(QU(sym))
      }
    }
    set

  private def extractQuString(quArray: js.Array[G.QU]): String =
    if (quArray != null && quArray.length > 0) {
      val raw = quArray(0)
      // Functional check: is it a primitive string or an object?
      if (js.typeOf(raw) == "string") raw.toString 
      else {
        val d = raw.asInstanceOf[js.Dynamic]
        if (!js.isUndefined(d.name)) d.name.toString
        else if (!js.isUndefined(d.value)) d.value.toString
        else ""
      }
    } else ""

  private def extractQuRefs(qurc: js.UndefOr[G.QuReferences]): QuReferences =
    qurc.toOption match {
      case Some(refsObj) =>
        val parsed = refsObj.quRefs.map { (r: G.QuReference) =>
          QuReference(
            refName = r.ref.asInstanceOf[js.Dynamic].ref.name.toString,
            qu = extractQuString(r.qu)
          )
        }
        QuReferences(LinkedHashSet.from(parsed))
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
           map.update("Clinical", section)

        case "Issues" =>
          val i = element.asInstanceOf[G.Issues]
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
                    refs = extractQuRefs(oc.qurc) // Uses the QuReference(String) logic
                  ))
                else None
              }
              
              NGO(
                name = ng.name,
                narratives = NL_STATEMENT.fromJsSeq(ng.narrative.toSeq),
                orders = LinkedHashSet.from(orderItems),
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