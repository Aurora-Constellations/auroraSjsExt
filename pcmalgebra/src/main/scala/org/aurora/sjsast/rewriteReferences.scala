package org.aurora.sjsast

import scala.collection.mutable.LinkedHashSet

object RewriteReferences:

  def addAliasToModule(module: ModulePCM, alias: String): ModulePCM =
    val issueNames: Set[String] = module.cio.values.collect {
      case i: Issues => i.coordinates.map(_.name) 
    }.flatten.toSet

    val targets = if (issueNames.nonEmpty) issueNames else Set(module.name)

    val newCio = module.cio.map { (key, section) =>
      key -> addAliasToCIO(section, alias, targets)
    }
    module.copy(cio = newCio)

  private def addAliasToCIO(section: CIO, alias: String, targets: Set[String]): CIO =
    section match
      case i: Issues => 
        val newCoords = i.coordinates.map(ic => addAliasToIssueCoord(ic, alias, targets))
        i.copy(coordinates = newCoords)
      
      case o: Orders => 
        val newNamedGroups = o.namedGroups.map(ngo => addAliasToNGO(ngo, alias, targets))
        o.copy(namedGroups = newNamedGroups)
      
      case c: Clinical => 
        val newNamedGroups = c.namedGroups.map(ngc => addAliasToNGC(ngc, alias, targets))
        c.copy(namedGroups = newNamedGroups)

  private def addAliasToIssueCoord(ic: IssueCoordinate, alias: String, targets: Set[String]): IssueCoordinate =
    val newRefs = transformQuReferences(ic.refs, alias, targets)
    ic.copy(refs = newRefs)

  private def addAliasToNGO(ngo: NGO, alias: String, targets: Set[String]): NGO =
    val newOrders = ngo.orders.map(oc => addAliasToOrderCoord(oc, alias, targets))
    val newRefs = transformQuReferences(ngo.refs, alias, targets)
    ngo.copy(orders = newOrders, refs = newRefs)

  private def addAliasToNGC(ngc: NGC, alias: String, targets: Set[String]): NGC =
    val newCoords = ngc.coordinates.map(cc => addAliasToClinicalCoord(cc, alias, targets))
    val newRefs = transformQuReferences(ngc.refs, alias, targets)
    ngc.copy(coordinates = newCoords, refs = newRefs)

  private def addAliasToClinicalCoord(cc: ClinicalCoordinate, alias: String, targets: Set[String]): ClinicalCoordinate =
    val newRefs = transformQuReferences(cc.refs, alias, targets)
    cc.copy(refs = newRefs)

  private def addAliasToOrderCoord(c: OrderCoordinate, alias: String, targets: Set[String]): OrderCoordinate =
    val newRefs = transformQuReferences(c.refs, alias, targets)
    c.copy(refs = newRefs)

  // Shared function to transform QuReferences
  private def transformQuReferences(refs: QuReferences, alias: String, targets: Set[String]): QuReferences =
    val newRefs = refs.refs.map { ref =>
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