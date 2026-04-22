package org.aurora.sjsast

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
    val newRefs = LHSet(ic.qurefs.toList.map(qurcs => transformQuReferences(qurcs, alias, targets))*)
    ic.copy(qurefs = newRefs)

  private def addAliasToNGO(ngo: NGO, alias: String, targets: Set[String]): NGO =
    val newOrders = ngo.ordercoord.map(oc => addAliasToOrderCoord(oc, alias, targets))
    val newRefs = LHSet(ngo.qurefs.toList.map(qurcs => transformQuReferences(qurcs, alias, targets))*)
    ngo.copy(ordercoord = newOrders, qurefs = newRefs)

  private def addAliasToNGC(ngc: NGC, alias: String, targets: Set[String]): NGC =
    // ngc.coordinates is now strictly LHSet[ClinicalItem], so no polymorphic matching is needed
    val newCoords = ngc.coordinates.map(ci => addAliasToClinicalItem(ci, alias, targets))
    val newRefs = LHSet(ngc.refs.toList.map(qurcs => transformQuReferences(qurcs, alias, targets))*)
    ngc.copy(coordinates = newCoords, refs = newRefs)

  private def addAliasToClinicalItem(cc: ClinicalItem, alias: String, targets: Set[String]): ClinicalItem =
    val newRefs = LHSet(cc.qurefs.toList.map(qurcs => transformQuReferences(qurcs, alias, targets))*)
    cc.copy(qurefs = newRefs)

  private def addAliasToOrderCoord(oc: OrderCoordinate, alias: String, targets: Set[String]): OrderCoordinate =
    val newRefs = LHSet(oc.qurefs.toList.map(qurcs => transformQuReferences(qurcs, alias, targets))*)
    oc.copy(qurefs = newRefs)

  private def transformQuReferences(qurefs: QuReferences, alias: String, targets: Set[String]): QuReferences =
    val newRefs = qurefs.qurc.map { ref =>
      if (targets.contains(ref.refName)) QuReference(refName = alias, qu = ref.qu)
      else ref
    }
    QuReferences(newRefs)