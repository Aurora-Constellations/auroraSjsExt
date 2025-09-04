package com.axiom


import org.scalajs.dom
import com.raquo.laminar.api.L.{*, given}
import com.axiom.ui.patienttracker.PatientTracker
import scala.scalajs.js.annotation.JSExportTopLevel
import com.axiom.model.shared.dto.Patient
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

@JSExportTopLevel("AxiomPatientTracker")
object AxiomPatientTracker :
  lazy val patientTracker:PatientTracker = new PatientTracker()
   
  case class PatientUI(unitNumber:String, accountNumber:String, lastName:String, firstName:String) 
  
  //TODO make better names
  def f(p:Patient):PatientUI = PatientUI(p.unitNumber,p.accountNumber,p.lastName,p.firstName)

  def consoleOut(msg: String): Unit = {
    dom.console.log(s"%c $msg","background: #222; color: #bada55")
  }


  def load() =    

    ModelFetch.fetchPatients.map{lp => lp.map {p => f(p)} }.foreach{ p => 
      patientTracker.populate(p)
    }

    consoleOut("fetched and populated and will be rendered!!")

    patientTracker.renderHtml
 
  def apply():Element = load()


    //TODO populating table model

