package com.axiom


import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import com.axiom.ui.*
import java.time.LocalDate
import java.time.LocalDateTime
import com.axiom.model.shared.dto._
import com.axiom.ui.BillingUIRenderer

object Main :
	def consoleOut(msg: String): Unit = {
		dom.console.log(s"%c $msg","background: #222; color: #bada55")
	}

	@main def entrypoint(): Unit = {
		consoleOut("Starting app...")

	
		val selectedPatientIdSignal = PatientsTable.selectedPatientIdVar.signal

		val accountsSignal = selectedPatientIdSignal.map {
		case Some(pid) => accounts.filter(_.patientId == pid)
		case None      => Nil
		}

		val encountersSignal = accountsSignal.map {
		case as if as.nonEmpty =>
			val activeAcc = as.find(_.endDate.isEmpty).map(_.accountId)
			activeAcc.map(accId => encounters.filter(_.accountId == accId)).getOrElse(Nil)
		case _ => Nil
		}

		val selectedEncounterVar = Var[Option[Long]](None)
		val selectedEncounterSignal = selectedEncounterVar.signal

		val billingsSignal = selectedEncounterSignal.map {
		case Some(eid) => billings.filter(_.encounterId == eid)
		case None      => Nil
		}

		val app = div(
			h2("Patient Billing Dashboard"),
			h3("Patients"),
			PatientsTable(patients),
			hr(),
			h3("Accounts"),
			AccountsTable.bind(accountsSignal),
			hr(),
			h3("Encounters"),
			EncountersTable.bind(encountersSignal, selectedEncounterVar), // same pattern
			hr(),
			h3("Billing Codes"),
			BillingCodesTable.bind(billingsSignal)
		)

		dom.document.querySelector("#app") match
			case null => consoleOut("No element with id 'app' found.")
			case el: dom.html.Element => render(el, app)
	}

