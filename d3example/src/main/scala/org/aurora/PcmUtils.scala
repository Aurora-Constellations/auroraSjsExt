package org.aurora

import org.aurora.sjsast.PCM
import org.aurora.sjsast.{Issues, IssueCoordinate, Clinical, OrderCoordinate, Orders, LHMap, NL_STATEMENT}
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import org.aurora.sjsast.LHSet

object PcmUtils {
  def getTopLevelChildren(node: Any): LHMap[String, OrderCoordinate | IssueCoordinate | NL_STATEMENT] =
    node match {
      case pcm: PCM =>
        val lhmap = LHMap.empty[String, OrderCoordinate | IssueCoordinate | NL_STATEMENT]

        val ocs = pcm.cio.get("Orders").collect{
          case o: Orders => o.ngo.flatMap(ngo => ngo.ordercoord)
        }.getOrElse(LHSet.empty[OrderCoordinate])        
        ocs.map(oc => lhmap.addOne((oc.name, oc)))

        val ics = pcm.cio.get("Issues").collect{
          case i: Issues => i.ic
        }.getOrElse(LHSet.empty[IssueCoordinate])
        ics.map(ic => lhmap.addOne((ic.name, ic)))

        lhmap

      case ic: IssueCoordinate => 
        val lhmap = LHMap.empty[String, OrderCoordinate | IssueCoordinate | NL_STATEMENT]
        ic.narratives.map(n => lhmap.addOne(n.name, n))
        lhmap
      case oc: OrderCoordinate => 
        val lhmap = LHMap.empty[String, OrderCoordinate | IssueCoordinate | NL_STATEMENT]
        oc.narratives.map(n => lhmap.addOne(n.name, n))
        lhmap
    }

  def getTargetsFromChildrenAndRefs(node: Any): LHMap[String, js.Array[String]] = 
    node match {
      case pcm: PCM => 
        val lhmap = LHMap.empty[String, js.Array[String]]
        val ocs = pcm.cio.get("Orders").collect{
          case o: Orders => o.ngo.flatMap(ngo => ngo.ordercoord)
        }.getOrElse(LHSet.empty[OrderCoordinate])  
        ocs.map(n => lhmap.addOne(n.name, getTargetsFromChildrenAndRefs(n).get(n.name).get))
        lhmap
      
      case oc: OrderCoordinate =>
        val nars = oc.narratives.map(n => n.name)
        val refs = oc.qurefs.flatMap(r => r.qurc.map(rc => rc.refName))
        LHMap((oc.name, nars.concat(refs).toJSArray))
    }
}
