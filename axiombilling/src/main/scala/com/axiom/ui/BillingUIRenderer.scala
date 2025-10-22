package com.axiom.ui

import scala.scalajs.js.annotation.JSExportTopLevel
import com.axiom.ui
import com.axiom.ui.BillingTracker
import java.time.LocalDateTime
import com.axiom.model.shared.dto.Patient
import java.time.LocalDate
// import org.w3c.dom.Element
import com.raquo.laminar.api.L.Element
import com.axiom.ui


@JSExportTopLevel("UIRenderer")
object BillingUIRenderer:
  lazy val billingTracker: BillingTracker = new BillingTracker()

   case class BPatientRow(
      accountNumber: String,
      firstName: String,
      sex: String,
      dob: Option[LocalDate],
      admitDate: Option[LocalDateTime],
      room: Option[String]
  )

  def bpatientRow(p: Patient): BPatientRow =
    BPatientRow(
      accountNumber = p.accountNumber,
      firstName = p.firstName,
      sex = p.sex,
      dob = p.dob,
      admitDate = p.admitDate,
      room = p.room
      
    )

  

   def load(patients: List[Patient]): Unit =
    val rows: List[BPatientRow] = patients.map(bpatientRow)
    billingTracker.populate(rows)       // <- GridT#populate (calls cctoData)
    


  def apply(patients: List[Patient]): Element = 
    load(patients)
    billingTracker.renderHtml