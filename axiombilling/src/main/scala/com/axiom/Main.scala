package com.axiom

import com.raquo.laminar.api.L._
import com.axiom.model.shared.dto.{Patient, Account, Encounter, Billing}
import com.axiom.ui._
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalajs.dom
import com.raquo.airstream.flatten.FlattenStrategy.allowFlatMap
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Main:

  @main def entrypoint(): Unit =
    val patientsVar   = Var(List.empty[Patient])
    val accountsVar   = Var(List.empty[Account])
    val encountersVar = Var(List.empty[Encounter])
    val billingsVar   = Var(List.empty[Billing])

    // Filter billings by selected encounter 
    val billingsForEncounterSignal: Signal[List[Billing]] =
      EncountersTable.selectedEncounterIdVar.signal
        .combineWith(billingsVar.signal)
        .map {
          case (Some(eid), all) => all.filter(_.encounterId == eid)
          case _                => Nil
        }

    // helper to parse datetime string
    def parseLdt(s: String): Option[LocalDateTime] =
      try Some(LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")))
      catch case _: Throwable => None

    val app =
      div(
        
        onMountCallback { ctx =>
          given Owner = ctx.owner

          // 1) load patients 
          EventStream.fromFuture(ModelFetch.fetchPatients)
            .foreach(patientsVar.set)

          // 2) when patient selected -> fetch accounts
          PatientsTable.selectedPatientIdVar.signal
            .changes
            .collect { case Some(pid) => pid }
            .flatMapSwitch(pid =>
              EventStream.fromFuture(ModelFetch.fetchAccountsByPatientId(pid))
            )
            .foreach { as =>
              accountsVar.set(as)
              encountersVar.set(Nil)
              billingsVar.set(Nil)
              AccountsTable.selectedAccountIdVar.set(None)
              EncountersTable.selectedEncounterIdVar.set(None)
            }

          // 3) when account selected -> fetch encounters
          AccountsTable.selectedAccountIdVar.signal
            .changes
            .collect { case Some(accId) => accId }
            .flatMapSwitch(accId =>
              EventStream.fromFuture(ModelFetch.fetchEncountersByAccountId(accId))
            )
            .foreach { es =>
              encountersVar.set(es)
              EncountersTable.selectedEncounterIdVar.set(None)
              billingsVar.set(Nil)
            }

          //4) When encounter selected → fetch all billings 
          EncountersTable.selectedEncounterIdVar.signal
            .changes
            .collect { case Some(_) => () }
            .take(1) // fetch all billings 
            .flatMap(_ =>
              EventStream.fromFuture(ModelFetch.fetchAllBillings)
            )
            .foreach(billingsVar.set)
        },

        h2("Patient Billing Dashboard"),

        h3("Patients"),
        PatientsTable.bindWithContextMenu(
          patientsSignal = patientsVar.signal,
          accountsVar    = accountsVar
        )(
          onViewAllAccounts = pid => {
            given Owner = unsafeWindowOwner
            AccountsTable.showAll()
            EventStream.fromFuture(ModelFetch.fetchAccountsByPatientId(pid))
              .foreach { as =>
                accountsVar.set(as)
                AccountsTable.selectedAccountIdVar.set(None)
                EncountersTable.selectedEncounterIdVar.set(None)
                ContextMenu.hide()
              }
          },
          onCreateAccount = pid => {
            given Owner = unsafeWindowOwner
            val start = LocalDateTime.now() 
            EventStream.fromFuture(ModelFetch.createAccount(pid, start))
              .flatMapSwitch { _ =>
                EventStream.fromFuture(ModelFetch.fetchAccountsByPatientId(pid))
              }
              .foreach { as =>
                accountsVar.set(as)
                AccountsTable.selectedAccountIdVar.set(None)
                EncountersTable.selectedEncounterIdVar.set(None)
                ContextMenu.hide()
              }
        },
          onViewActiveAccount = accId => {
            AccountsTable.showActive()
            AccountsTable.selectedAccountIdVar.set(Some(accId))
            ContextMenu.hide()
          }
        ),

        hr(),

        h3("Accounts"),
        AccountsTable.bindWithContextMenu(accountsVar.signal)(
          onCreateEncounter = accId => {
            val docStr   = dom.window.prompt("Doctor ID (number):", "")
            val startStr = dom.window.prompt("Start datetime (yyyy-MM-ddTHH:mm):", "")

            if (docStr == null || startStr == null) () // user cancelled
            else {
              val doctorIdOpt = docStr.trim match
                case s if s.nonEmpty =>
                  try Some(s.toLong) catch case _: Throwable => None
                case _ => None
              val startOpt = parseLdt(startStr.trim)

              (doctorIdOpt, startOpt) match
                case (Some(doctorId), Some(start)) =>
                  given Owner = unsafeWindowOwner
                  EventStream.fromFuture(ModelFetch.createEncounter(accId, doctorId, start, Nil))
                    .flatMapSwitch(_ => EventStream.fromFuture(ModelFetch.fetchEncountersByAccountId(accId)))
                    .foreach { es =>
                      encountersVar.set(es)
                      AccountsTable.selectedAccountIdVar.set(Some(accId))
                    }
                case _ =>
                  dom.window.alert("Invalid Doctor ID or datetime. Use yyyy-MM-ddTHH:mm")
            }
          },
          onViewAllEncounters = accId => {
            AccountsTable.selectedAccountIdVar.set(Some(accId))
          }
        ),

        hr(),

        h3("Encounters"),
        EncountersTable.bind(encountersVar.signal),

        hr(),

        h3("Billing Codes"),
        BillingCodesTable.bind(billingsForEncounterSignal),

        
        
        ContextMenu.view
      )

    render(dom.document.querySelector("#app"), app)
