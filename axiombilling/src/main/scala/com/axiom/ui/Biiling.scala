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

val encountersVar = Var[List[Encounter]](Nil)
val selectedEncounterIdVar = Var[Option[Long]](None)

val billingsVar = Var[List[Billing]](Nil)
val billingCodesVar = Var[List[BillingCode]](Nil)

def patientTable(patients: Signal[List[Patient]]): HtmlElement = {
  val selectedIdSignal = selectedPatientIdVar.signal

  table(
    cls := "patient-table",
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
            val isSelected = selectedIdOpt.contains(patient.accountNumber.toLongOption.getOrElse(-1L))

            tr(
              cls.toggle("selected") <-- Val(isSelected),
              onClick --> { _ =>
                selectedPatientIdVar.set(Some(patient.accountNumber.toLong))
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
    child.text <-- account.map {
      case Some(acc) => s"Active Account ID: ${acc.accountId}"
      case None => "No active account"
    }
  )


val encounterOptionsSignal: Signal[List[Encounter]] =
  activeAccountSignal.combineWith(encountersVar.signal).map {
    case (Some(account), encounters) =>
      encounters.filter(_.accountId == account.accountId)
    case _ => Nil
  }

def encounterSelect(encounters: Signal[List[Encounter]]): HtmlElement =
  select(
    onChange.mapToValue.map(_.toLong) --> selectedEncounterIdVar.writer.contramap(Some[Long](_)),
    children <-- encounters.map(_.map { encounter =>
      option(
        value := encounter.encounterId.toString,
        s"Encounter ${encounter.encounterId}"
      )
    })
  )

val billingForEncounterSignal: Signal[List[Billing]] =
  selectedEncounterIdVar.signal.combineWith(billingsVar.signal).map {
    case (Some(eid), billings) =>
      billings.filter(_.encounterId == eid)
    case _ => Nil
  }

def billingList(billings: Signal[List[Billing]]): HtmlElement =
  ul(
    children <-- billings.map(_.map { billing =>
      li(s"Billing Code: ${billing.billingCode}, Units: ${billing.unitCount}")
    })
  )
