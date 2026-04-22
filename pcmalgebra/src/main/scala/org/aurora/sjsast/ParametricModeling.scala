package org.aurora.sjsast

object ParametricModeling:

    /**
     * Logic: If age is defined (exists), return PCM as is.
     * If age is not defined (missing), return PCM with age as "???".
     */
    def applyAgeConstraint(pcm: PCM): PCM =
        val clinicalSection = pcm.cio.get("Clinical").collect { case c: Clinical => c }
        
        clinicalSection match
            case Some(c) if hasAgeValue(c) => pcm // If age is defined, it works.
            case Some(c) => 
                pcm.copy(cio = pcm.cio.updated("Clinical", injectAge(c)))
            case None    => 
                pcm.copy(cio = pcm.cio.updated("Clinical", createClinicalWithPlaceholder()))

    private def hasAgeValue(clinical: Clinical): Boolean =
        clinical.ngc.exists(_.coordinates.exists {
            case ci: ClinicalItem if ci.name.toLowerCase == "age" && ci.values.nonEmpty => true
            case _ => false
        })
        // clinical.ngc.exists(_.coordinates.exists(_.name.toLowerCase == "age"))

    private def injectAge(clinical: Clinical): Clinical =
        val ageCoord = ClinicalItem(
            name = "age", 
            values = List(SingleValueUnit(StringValue("???"), "_"))
        )
        // Inject into the first group if it exists
        clinical.ngc.headOption match
            case Some(first) =>
                val updatedFirst = first.copy(coordinates = first.coordinates + ageCoord)
                clinical.copy(ngc = LHSet(updatedFirst) ++ clinical.ngc.tail)
            case None => createClinicalWithPlaceholder()

    private def createClinicalWithPlaceholder(): Clinical =
        Clinical(
            name = "Clinical",
            ngc = LHSet(
                NGC(
                    name = "Demographics:",
                    coordinates = LHSet(
                        ClinicalItem(name = "age", values = List(SingleValueUnit(StringValue("???"), "_")))
                    )
                )
            )
        )