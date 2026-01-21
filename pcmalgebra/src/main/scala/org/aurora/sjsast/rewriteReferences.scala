package org.aurora.sjsast

import scala.collection.mutable.LinkedHashSet

object RewriteReferences:

  def addAliasToCIO(section: CIO, alias: String, targets: Set[String]): CIO =
    section match
      case i: Issues => 
        val newCoords = i.ic.map(ic => addAliasToIssueCoord(ic, alias, targets))
        i.copy(ic = newCoords)
      
      case o: Orders => 
        val newNamedGroups = o.ngo.map(ngo => addAliasToNGO(ngo, alias, targets))
        o.copy(ngo = newNamedGroups)
      
      case c: Clinical => 
        val newNamedGroups = c.ngc.map(ngc => addAliasToNGC(ngc, alias, targets))
        c.copy(ngc = newNamedGroups)

  private def addAliasToIssueCoord(ic: IssueCoordinate, alias: String, targets: Set[String]): IssueCoordinate =
    val newRefs = transformQuReferences(ic.qurefs, alias, targets)
    ic.copy(qurefs = newRefs)

  private def addAliasToNGO(ngo: NGO, alias: String, targets: Set[String]): NGO =
    val newOrders = ngo.ordercoord.map(oc => addAliasToOrderCoord(oc, alias, targets))
    val newRefs = transformQuReferences(ngo.qurefs, alias, targets)
    ngo.copy(ordercoord = newOrders, qurefs = newRefs)

  private def addAliasToNGC(ngc: NGC, alias: String, targets: Set[String]): NGC =
    val newCoords = ngc.coordinates.map(cc => addAliasToClinicalCoord(cc, alias, targets))
    val newRefs = transformQuReferences(ngc.refs, alias, targets)
    ngc.copy(coordinates = newCoords, refs = newRefs)

  private def addAliasToClinicalCoord(cc: ClinicalCoordinate, alias: String, targets: Set[String]): ClinicalCoordinate =
    val newRefs = transformQuReferences(cc.qurefs, alias, targets)
    cc.copy(qurefs = newRefs)

  private def addAliasToOrderCoord(oc: OrderCoordinate, alias: String, targets: Set[String]): OrderCoordinate =
    val newRefs = transformQuReferences(oc.qurefs, alias, targets)
    oc.copy(qurefs = newRefs)

  // Shared function to transform QuReferences
  private def transformQuReferences(qurefs: QuReferences, alias: String, targets: Set[String]): QuReferences =
    val newRefs = qurefs.qurc.map { ref =>
      val matchResult = findMatchingTarget(ref.refName, targets)
      
      matchResult match
        case Some(matchedTarget) =>
          // Keep the same QU, just replace the refName with alias
          QuReference(refName = alias, qu = ref.qu)
        case None =>
          ref
    }
    
    QuReferences(newRefs)

  // Simplified: now we don't look for prefix in refName, it's in QU
  private def findMatchingTarget(refName: String, targets: Set[String]): Option[String] =
    if (targets.contains(refName)) Some(refName) else None