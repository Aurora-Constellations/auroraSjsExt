package com.axiom.ui

import com.raquo.laminar.api.L._
import com.axiom.model.shared.dto.Patient
import java.time.{LocalDate, LocalDateTime}
import com.axiom.shared.table.TableDerivation.given
import com.axiom.model.shared.dto.Account
import org.scalajs.dom

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

// Right-click menu 
  def bindWithContextMenu(
    patientsSignal: Signal[List[Patient]],
    accountsVar: Var[List[Account]]
  )(
    onViewAllAccounts: Long => Unit,
    onCreateAccount: Long => Unit,
    onViewActiveAccount: Long => Unit
  ): Element =
    val current = Var(List.empty[Patient])

    div(
      table.render(
        onRowClick = Some(i => selectedPatientIdVar.set(Some(current.now()(i).id))),
        onRowContextMenu = Some { (i, e: dom.MouseEvent) =>
          val ps = current.now()
          if (i >= 0 && i < ps.length) {
            val pid = ps(i).id
            val activeAccIdOpt =
              accountsVar.now().find(a => a.patientId == pid && a.endDate.isEmpty).map(_.accountId)

            val items =
              List(
                ContextMenu.Item("View all accounts", () => onViewAllAccounts(pid)),
                ContextMenu.Item("Create account",   () => onCreateAccount(pid))
              ) ++ activeAccIdOpt.toList.map(accId =>
                ContextMenu.Item("View active account", () => onViewActiveAccount(accId))
              )

            ContextMenu.show(e.clientX, e.clientY, items)
          }
        }
      ),
      patientsSignal --> { ps =>
        val limited = ps.take(5)
        current.set(limited) 
        table.populate(limited.map(p => PatientRow(p.accountNumber, p.firstName, p.sex, p.dob, p.admitDate, p.room)))
        selectedPatientIdVar.set(None)
      }
    )