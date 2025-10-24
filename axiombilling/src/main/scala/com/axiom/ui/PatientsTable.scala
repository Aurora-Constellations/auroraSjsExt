package com.axiom.ui


import com.raquo.laminar.api.L._
import com.axiom.model.shared.dto.Patient
import java.time.{LocalDate, LocalDateTime}
import com.axiom.shared.table.TableDerivation.derived

case class PatientRow(
  accountNumber: String,
  firstName: String,
  sex: String,
  dob: Option[LocalDate],
  admitDate: Option[LocalDateTime],
  room: Option[String]
)

object PatientsTable:
  private val table = ReactiveTable[PatientRow]
  val selectedPatientIdVar: Var[Option[Long]] = Var(None)

  def apply(patients: List[Patient]): Element =
    val rows = patients.map(p => PatientRow(
      p.accountNumber, p.firstName, p.sex, p.dob, p.admitDate, p.room
    ))
    table.populate(rows)
    table.render(onRowClick = Some(i => selectedPatientIdVar.set(Some(patients(i).id))))
