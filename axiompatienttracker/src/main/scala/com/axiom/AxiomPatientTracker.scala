package com.axiom

import java.time.LocalDateTime
import org.scalajs.dom
import com.raquo.laminar.api.L.{*, given}
import com.axiom.ui.patienttracker.PatientTracker
import scala.scalajs.js.annotation.JSExportTopLevel
import com.axiom.model.shared.dto.Patient
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global
import com.axiom.ui.patienttracker.utils.Status
import com.axiom.ui.patienttracker.utils.StatusIcons

@JSExportTopLevel("AxiomPatientTracker")
object AxiomPatientTracker:
  lazy val patientTracker: PatientTracker = new PatientTracker()

  //TODO why does it take time and cognitive load to find this?
  //ALSO PERHAPS THE ABSTRACTION COULD USE A TYPE ALIAS THAT TRIGGERS FUTURE DESIGNERS TO create the case class Row Representation
  case class PatientRow(
      status: HtmlElement,
      accountNumber: String,
      unitNumber: String,
      lastName: String,
      firstName: String,
      sex: String,
      admitDate: Option[LocalDateTime],
      Floor: Option[String],
      details: HtmlElement
  )

  def patientRow(p: Patient): PatientRow =
    val status: Status =
    Status.fromCode(p.flag.fold("0")(_.toString))   // None -> "0"
    PatientRow(
      status = StatusIcons.render(status),
      accountNumber = p.accountNumber,
      unitNumber = p.unitNumber,
      lastName = p.lastName,
      firstName = p.firstName,
      sex = p.sex,
      admitDate = p.admitDate,
      Floor = p.floor,
      details = patientTracker.renderActionButtons(p.unitNumber)
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
