package com.axiom

import java.time.LocalDateTime
import org.scalajs.dom
import com.raquo.laminar.api.L.{*, given}
import com.axiom.ui.patienttracker.PatientTracker
import scala.scalajs.js.annotation.JSExportTopLevel
import com.axiom.model.shared.dto.Patient
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

@JSExportTopLevel("AxiomPatientTracker")
object AxiomPatientTracker:
  lazy val patientTracker: PatientTracker = new PatientTracker()

 
  case class PatientRow(
      status: String,
      accountNumber: String,
      unitNumber: String,
      lastName: String,
      firstName: String,
      sex: String,
      admitDate: Option[LocalDateTime],
      Floor: Option[String],
      details: HtmlElement
  )

  def patientRow(p: Patient): PatientRow = PatientRow(
    p.flag.map(_.toString).getOrElse("0"),
    p.accountNumber,
    p.unitNumber,
    p.lastName,
    p.firstName,
    p.sex,
    p.admitDate,
    p.floor,
    patientTracker.renderActionButtons(p.unitNumber)
  )

  def consoleOut(msg: String): Unit = {
    dom.console.log(s"%c $msg", "background: #222; color: #bada55")
  }

  def load() =
    ModelFetch.fetchPatients.map { lp => lp.map { p => patientRow(p) } }.foreach { p =>
      patientTracker.populate(p)
    }
    consoleOut("fetched and populated and will be rendered!!")
    patientTracker.renderHtml

  def apply(): Element = load()
