package org.aurora.sjsast

case class ModulePCM(module: Module):
  
  /**
   * Convert the module to a PCM without any aliasing
   */
  def toPCM: PCM = 
    PCM(cio = module.cio)
  
  /**
   * Convert the module to a PCM with aliased references
   * This extracts the original issue name from the module's Issues section,
   * then rewrites all references from that original name to the provided alias
   */
  def toPCM(aliasName: String): PCM = 
    // Extract the original issue name from the module's Issues section
    val originalIssueNames: Set[String] = module.cio.values.collect {
      case i: Issues => i.ic.map(_.name)
    }.flatten.toSet
    
    // If no issues found, use the module name as fallback
    val targets = if (originalIssueNames.nonEmpty) originalIssueNames else Set(module.name)
    
    // Rewrite all references in the CIO sections
    val aliasedCIO = module.cio.map { (key, section) =>
      key -> RewriteReferences.addAliasToCIO(section, aliasName, targets)
    }
    
    PCM(cio = aliasedCIO)