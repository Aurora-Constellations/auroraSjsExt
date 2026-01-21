package org.aurora.sjsast

import org.aurora.sjsast.{GenAst => G}
import scala.scalajs.js

case class Module(
    name: String,
    cio: LHMap[String, CIO] = LHMap()
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
    LHSet.from(quArray.toSeq.map(QU(_)))

  private def extractQU(quArray: js.Array[G.QU]): QU =
    QU(quArray)

  private def extractQuRefs(qurc: js.UndefOr[G.QuReferences]): LHSet[QuReferences] =
    qurc.toOption match {
      case Some(refsObj) =>
        LHSet(QuReferences(refsObj))
      case None => LHSet()
    }

  private def cioFromElements(elements: js.Array[Any]): LHMap[String, CIO] =
    val map = LHMap[String, CIO]()

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
                   narratives = NL_STATEMENT(cc.narrative.toSeq),
                   qurefs = extractQuRefs(cc.qurc),
                   qu = extractQU(cc.qu)
                 ))
               else None
             }
             
             NGC(
               name = ng.name, 
               narratives = NL_STATEMENT(ng.narrative.toSeq),
               coordinates = LHSet.from(coords),
               refs = extractQuRefs(ng.asInstanceOf[js.Dynamic].qurc.asInstanceOf[js.UndefOr[G.QuReferences]])
             )
           }

           val section = Clinical(
             narratives = NL_STATEMENT(c.narrative.toSeq),
             ngc = LHSet.from(groups)
           )
           map.update("Clinical", section)

        case "Issues" =>
          val i = element.asInstanceOf[G.Issues]
          val coords = i.coord.map { ic =>
             IssueCoordinate(
               name = ic.name,
               narratives = NL_STATEMENT(ic.narrative.toSeq),
               qurefs = extractQuRefs(ic.qurc),
               qu = extractQU(ic.qu)
             )
          }
          
          val section = Issues(
            narratives = NL_STATEMENT(i.narrative.toSeq),
            ic = LHSet.from(coords)
          )
          map.update("Issues", section)

        case "Orders" =>
            val o = element.asInstanceOf[G.Orders]
            val groups = o.namedGroups.map { ngo =>
              val orderItems = ngo.orders.flatMap { item =>
                if getType(item) == "OrderCoordinate" then
                  val oc = item.asInstanceOf[G.OrderCoordinate]
                  Some(OrderCoordinate(
                    name = oc.name,
                    narratives = NL_STATEMENT(oc.narrative.toSeq),
                    qurefs = extractQuRefs(oc.qurc)
                  ))
                else None
              }
              
              NGO(
                name = ngo.name,
                narratives = NL_STATEMENT(ngo.narrative.toSeq),
                ordercoord = LHSet.from(orderItems),
                qurefs = extractQuRefs(ngo.asInstanceOf[js.Dynamic].qurc.asInstanceOf[js.UndefOr[G.QuReferences]]),
                qu = extractQuSet(ngo.asInstanceOf[js.Dynamic].qu.asInstanceOf[js.Array[G.QU]])
              )
            }

            val section = Orders(
              narratives = NL_STATEMENT(o.narrative.toSeq),
              ngo = LHSet.from(groups)
            )
            map.update("Orders", section)
        case _ => // Ignore
      }
    map