package com.axiom

import com.raquo.laminar.api.L._
import com.axiom.model.shared.dto.{Patient, Account, Encounter, Billing}
import com.axiom.ui._
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalajs.dom

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

    val app =
      div(
        
        onMountCallback { ctx =>
          given Owner = ctx.owner

          // 1) load patients 
          EventStream.fromFuture(ModelFetch.fetchPatients)
            .foreach(patientsVar.set)

          // 2) load all billings  ( filter client-side by encounterId)
          EventStream.fromFuture(ModelFetch.fetchAllBillings)
            .foreach(billingsVar.set)

          // 3) when patient selected -> fetch accounts
          PatientsTable.selectedPatientIdVar.signal
            .changes
            .collect { case Some(pid) => pid }
            .flatMapSwitch(pid =>
              EventStream.fromFuture(ModelFetch.fetchAccountsByPatientId(pid))
            )
            .foreach { as =>
              accountsVar.set(as)
              encountersVar.set(Nil)
              AccountsTable.selectedAccountIdVar.set(None)
              EncountersTable.selectedEncounterIdVar.set(None)
            }

          // 4) when account selected -> fetch encounters
          AccountsTable.selectedAccountIdVar.signal
            .changes
            .collect { case Some(accId) => accId }
            .flatMapSwitch(accId =>
              EventStream.fromFuture(ModelFetch.fetchEncountersByAccountId(accId))
            )
            .foreach { es =>
              encountersVar.set(es)
              EncountersTable.selectedEncounterIdVar.set(None)
            }
        },

        h2("Patient Billing Dashboard"),

        h3("Patients"),
        PatientsTable.bind(patientsVar.signal),

        hr(),

        h3("Accounts"),
        AccountsTable.bind(accountsVar.signal),

        hr(),

        h3("Encounters"),
        EncountersTable.bind(encountersVar.signal),

        hr(),

        h3("Billing Codes"),
        BillingCodesTable.bind(billingsForEncounterSignal)
      )

    render(dom.document.querySelector("#app"), app)
