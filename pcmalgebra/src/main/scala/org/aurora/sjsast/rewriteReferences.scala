package org.aurora.sjsast

import scala.collection.mutable.LinkedHashSet

object RewriteReferences:

  def addAliasToModule(module: ModulePCM, alias: String): ModulePCM =
    val newCio = module.cio.map { (key, section) =>
      key -> addAliasToCIO(section, alias, module.name) // Pass module.name (e.g. "CHF")
    }
    module.copy(cio = newCio)

  private def addAliasToCIO(section: CIO, alias: String, moduleName: String): CIO = section match
    case o: Orders =>
      o.copy(namedGroups = o.namedGroups.map(ng => addAliasToNGO(ng, alias, moduleName)))
    case c: Clinical =>
      c.copy(namedGroups = c.namedGroups.map(ng => addAliasToClinicalGroup(ng, alias, moduleName)))
    case i: Issues =>
      i.copy(coordinates = i.coordinates.map(c => addAliasToCoord(c, alias, moduleName)))

  private def addAliasToNGO(ng: NGO, alias: String, moduleName: String): NGO =
    // Update the orders within the group and carry over the group's own references/qualifiers
    ng.copy(
      orders = ng.orders.map(o => addAliasToCoord(o, alias, moduleName)),
      // Optional: If you want the NGO itself to have an alias reference:
      refs = QuReferences(LinkedHashSet(QuReference(alias, ""))) 
    )

  private def addAliasToClinicalGroup(ng: NGC, alias: String, moduleName: String): NGC =
    ng.copy(coordinates = ng.coordinates.map(c => addAliasToCoord(c, alias, moduleName)))

  private def addAliasToCoord(c: OrderCoordinate, alias: String, moduleName: String): OrderCoordinate =
    // Find the symbol attached to the module name inside the parentheses (e.g., '?' from ?chf)
    val existingQu = c.refs.refs
      .find(_.refName == moduleName)
      .map(_.qu)
      .getOrElse("")

    // Create the new alias reference with that preserved string symbol
    val newAliasRef = QuReference(refName = alias, qu = existingQu)
    
    // Update coordinate with the new alias reference
    c.copy(refs = QuReferences(LinkedHashSet(newAliasRef)))

  private def addAliasToCoord(c: IssueCoordinate, alias: String, moduleName: String): IssueCoordinate =
    val existingQu = c.refs.refs.find(_.refName == moduleName).map(_.qu).getOrElse("")
    c.copy(refs = addRef(c.refs, alias, existingQu))

  private def addAliasToCoord(c: ClinicalCoordinate, alias: String, moduleName: String): ClinicalCoordinate =
    val existingQu = c.refs.refs.find(_.refName == moduleName).map(_.qu).getOrElse("")
    c.copy(refs = addRef(c.refs, alias, existingQu))

  private def addRef(refs: QuReferences, alias: String, qu: String): QuReferences =
    val newRef = QuReference(refName = alias, qu = qu)
    // We clear the old module name reference and keep only the aliased one for the merge
    QuReferences(LinkedHashSet(newRef))