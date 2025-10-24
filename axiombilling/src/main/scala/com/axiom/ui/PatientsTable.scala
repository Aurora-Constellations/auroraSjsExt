package com.axiom.ui

import com.raquo.laminar.api.L._
import com.axiom.model.shared.dto.Patient
import java.time.{LocalDate, LocalDateTime}
import com.axiom.shared.table.TableDerivation.given

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

  def bind(patientsSignal: Signal[List[Patient]]): Element =
    val current = Var(List.empty[Patient])
    div(
      table.render(onRowClick = Some(i => selectedPatientIdVar.set(Some(current.now()(i).id)))),
      patientsSignal --> { ps =>
        current.set(ps)
        table.populate(ps.map(p => PatientRow(p.accountNumber, p.firstName, p.sex, p.dob, p.admitDate, p.room)))
        selectedPatientIdVar.set(None)
      }
    )
