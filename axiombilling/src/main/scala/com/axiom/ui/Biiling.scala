package com.axiom.ui

import com.axiom.model.shared.dto.{Patient, Billing, Account, DiagnosticCodes, Doctor, BillingCode, Encounter}
import org.scalajs.dom
import com.raquo.laminar.api.L.{*, given}

val patientsVar = Var[List[Patient]](Nil)
val selectedPatientIdVar = Var[Option[Long]](None)

val accountsVar = Var[List[Account]](Nil)
val activeAccountSignal: Signal[Option[Account]] =
  selectedPatientIdVar.signal.combineWith(accountsVar.signal).map {
    case (Some(pid), accounts) =>
      accounts.find(acc => acc.patientId == pid && acc.endDate.isEmpty)
    case _ => None
  }

val accountCountSignal: Signal[Int] =
  selectedPatientIdVar.signal.combineWith(accountsVar.signal).map {
    case (Some(pid), accounts) => accounts.count(_.patientId == pid)
    case _ => 0
  }

val encountersVar = Var[List[Encounter]](Nil)
val selectedEncounterIdVar = Var[Option[Long]](None)

val billingsVar = Var[List[Billing]](Nil)
val billingCodesVar = Var[List[BillingCode]](Nil)

def patientTable(patients: Signal[List[Patient]]): HtmlElement = {
  val selectedIdSignal = selectedPatientIdVar.signal

  // When patient changes, clear encounter selection
  selectedPatientIdVar.signal.changes.foreach { _ =>
    selectedEncounterIdVar.set(None)
  }(unsafeWindowOwner)

  table(
    cls := "my-table",
    thead(
      tr(
        th("Account #"),
        th("Name"),
        th("Sex"),
        th("DOB"),
        th("Admit Date"),
        th("Room")
      )
    ),
    tbody(
      children <-- patients.combineWith(selectedIdSignal).map {
        case (patList, selectedIdOpt) =>
          patList.map { patient =>
            val isSelected = selectedIdOpt.contains(patient.id)

            tr(
              cls.toggle("selected") <-- Val(isSelected),
              onClick --> { _ =>
                selectedPatientIdVar.set(Some(patient.id))
              },
              td(patient.accountNumber),
              td(s"${patient.firstName} ${patient.lastName}"),
              td(patient.sex),
              td(patient.dob.map(_.toString).getOrElse("-")),
              td(patient.admitDate.map(_.toString).getOrElse("-")),
              td(s"${patient.floor.getOrElse("")}-${patient.room.getOrElse("")}-${patient.bed.getOrElse("")}")
            )
          }
      }
    )
  )
}


def activeAccountDisplay(account: Signal[Option[Account]]): HtmlElement =
  div(
    children <-- account.map {
      case Some(acc) =>
        Seq(
          div(s"Active Account ID: ${acc.accountId}"),
          div(s"Note: ${acc.notes.getOrElse("-")}")
        )
      case None =>
        Seq(
          div("No active account")
        )
    }
  )

val encounterOptionsSignal: Signal[List[Encounter]] =
  activeAccountSignal.combineWith(encountersVar.signal).map {
    case (Some(account), encounters) =>
      encounters.filter(_.accountId == account.accountId)
    case _ => Nil
  }

val billingForEncounterSignal: Signal[List[Billing]] =
  selectedEncounterIdVar.signal.combineWith(billingsVar.signal).map {
    case (Some(eid), billings) =>
      billings.filter(_.encounterId == eid)
    case _ => Nil
  }

def encounterTable(encounters: Signal[List[Encounter]]): HtmlElement = {
  val selectedIdSignal = selectedEncounterIdVar.signal

  table(
    cls := "my-table",
    thead(
      tr(
        th("Encounter ID"),
        th("Start Date"),
        th("End Date")
      )
    ),
    tbody(
      children <-- encounters.combineWith(selectedIdSignal).map {
        case (encList, selectedIdOpt) =>
          encList.map { enc =>
            val isSelected = selectedIdOpt.contains(enc.encounterId)

            tr(
              cls.toggle("selected") <-- Val(isSelected),
              onClick --> { _ =>
                selectedEncounterIdVar.set(Some(enc.encounterId))
              },
              td(enc.encounterId.toString),
              td(enc.startDate.toString),
              td(enc.endDate.map(_.toString).getOrElse("-")),
            )
          }
      }
    )
  )
}

def billingList(billings: Signal[List[Billing]]): HtmlElement =
  table(
    cls := "my-table",
    thead(
      tr(
        th("Billing ID"),
        th("Billing Code"),
        th("Diagnostic Code"),
        th("Recorded Time"),
        th("Units"),
        th("Notes")
      )
    ),
    tbody(
      children <-- billings.map(_.map { billing =>
        tr(
          td(billing.billingId.toString),
          td(billing.billingCode),
          td(billing.diagnosticCode),
          td(billing.recordedTime.map(_.toString).getOrElse("-")),
          td(billing.unitCount.toString),
          td(billing.Notes.getOrElse("-"))
        )
      })
    )
  )

