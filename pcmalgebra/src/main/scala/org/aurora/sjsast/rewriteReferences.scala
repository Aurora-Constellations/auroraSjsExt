package org.aurora.sjsast

import scala.collection.mutable.LinkedHashSet

object RewriteReferences:

  def addAliasToModule(module: ModulePCM, alias: String): ModulePCM =
    // 1. Extract the internal Issue names defined in this module (e.g., "chf" from "chf :Congestive_heart_failure")
    val issueNames: Set[String] = module.cio.values.collect {
      case i: Issues => i.coordinates.map(_.name) 
    }.flatten.toSet
    // 2. Define the targets we are looking for in the references.
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
      // Check if this reference's refName matches any target (with or without prefix)
      val matchResult = findMatchingTarget(ref.refName, targets)
      
      matchResult match
        case Some((prefix, matchedTarget)) =>
          // Replace the matched target with the alias, preserving the prefix
          QuReference(refName = s"$prefix$alias", qu = ref.qu)
        case None =>
          // No match, keep the reference as is
          ref
    }
    QuReferences(newRefs)

  // Helper function to find if refName matches any target, with or without a prefix
  private def findMatchingTarget(refName: String, targets: Set[String]): Option[(String, String)] =
    // First check for exact match (no prefix)
    if (targets.contains(refName)) {
      return Some(("", refName))
    }
    // Then check if refName has a single-character prefix followed by a target
    if (refName.length > 1) {
      val potentialPrefix = refName.head.toString
      val potentialTarget = refName.tail
      
      if (targets.contains(potentialTarget)) {
        return Some((potentialPrefix, potentialTarget))
      }
    }
    None