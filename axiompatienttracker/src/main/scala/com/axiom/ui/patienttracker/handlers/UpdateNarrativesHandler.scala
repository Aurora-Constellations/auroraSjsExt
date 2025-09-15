package com.axiom.ui.patienttracker.handlers

import com.axiom.ModelFetch
import com.axiom.ui.patienttracker.sendResponseToVSCode
import com.axiom.messaging.*
import scala.concurrent.ExecutionContext.Implicits.global
import com.axiom.AxiomPatientTracker
import com.axiom.ui.patienttracker.PatientTracker

object UpdateNarrativesHandler extends MessageHandler[UpdateNarrativesMsg] {
  def handle(msg: UpdateNarrativesMsg): Unit = {
    ModelFetch.addNarrativesFlag(msg.unitNumber, msg.flag).map {
      case Some(_) =>
        println(s"Narrative Flag updated successfully for unit number: ${msg.unitNumber}")
        sendResponseToVSCode(Response(MessagingCommands.UpdatedNarratives, UpdatedNarratives(s"Narratives updated successfully for ${msg.unitNumber}.aurora")))
        ModelFetch.fetchPatients.foreach{ patients => 
            val ui = patients.map(AxiomPatientTracker.patientRow)  
            val tracker = AxiomPatientTracker.patientTracker
            tracker.refreshAndKeepSearch(ui)
        }
        
      case None =>
        println(s"Failed to update Narrative Flag for unit number: ${msg.unitNumber}")
        sendResponseToVSCode(Response(MessagingCommands.UpdatedNarratives, UpdatedNarratives(s"Failed to update Narratives for ${msg.unitNumber}.aurora")))
    }
  }
}
