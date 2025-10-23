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

		val accounts = List(
			Account(
				accountId = 1L,
				patientId = 1L,
				startDate = LocalDateTime.of(2025, 7, 1, 10, 0),
				endDate = None // Active
			),
			Account(
				accountId = 2L,
				patientId = 1L,
				startDate = LocalDateTime.of(2024, 1, 1, 9, 0),
				endDate = Some(LocalDateTime.of(2024, 2, 1, 12, 0))
			),
			Account(
				accountId = 3L,
				patientId = 2L,
				startDate = LocalDateTime.of(2025, 7, 5, 14, 30),
				endDate = None // Active
			)
		)

		val encounters = List(
			Encounter(
				encounterId = 101L,
				accountId = 1L,
				doctorId = 10001L,
				startDate = LocalDateTime.of(2025, 7, 2, 9, 0)
			),
			Encounter(
				encounterId = 102L,
				accountId = 1L,
				doctorId = 10002L,
				startDate = LocalDateTime.of(2025, 7, 3, 14, 0)
			),
			Encounter(
				encounterId = 201L,
				accountId = 3L,
				doctorId = 10003L,
				startDate = LocalDateTime.of(2025, 7, 6, 11, 0)
			)
		)

		val billings = List(
			Billing(
				billingId = 1L,
				encounterId = 101L,
				billingCode = "PROC1001",
				diagnosticCode = "DX001",
				recordedTime = Some(LocalDateTime.of(2025, 7, 2, 9, 30)),
				unitCount = 1,
				Notes = Some("Routine checkup")
			),
			Billing(
				billingId = 2L,
				encounterId = 102L,
				billingCode = "PROC1002",
				diagnosticCode = "DX002",
				recordedTime = Some(LocalDateTime.of(2025, 7, 3, 15, 0)),
				unitCount = 2,
				Notes = Some("Chest X-ray")
			),
			Billing(
				billingId = 3L,
				encounterId = 201L,
				billingCode = "PROC1003",
				diagnosticCode = "DX003",
				recordedTime = Some(LocalDateTime.of(2025, 7, 6, 11, 15)),
				unitCount = 3,
				Notes = Some("MRI Brain")
			)
		)


		// Populate initial vars
		// patientsVar.set(patients)
		// accountsVar.set(accounts)
		// encountersVar.set(encounters)
		// billingsVar.set(billings)

		val app = div(
			h2("Patient Billing Dashboard"),

			h3("Patients"),
			BillingUIRenderer(patients)

			// hr(),

			// h3(
			// 	child.text <-- accountCountSignal.map(count =>
			// 		s"Active Account (Total # of accounts: $count)"
			// 	)
			// ),
			// activeAccountDisplay(activeAccountSignal),

			// h3("Select Encounter"),
			// encounterTable(encounterOptionsSignal),

			// hr(),

			// h3("Billing Codes for Encounter"),
			// billingList(billingForEncounterSignal)
		)

		dom.document.querySelector("#app") match
			case null => consoleOut("No element with id 'app' found.")
			case el: dom.html.Element => render(el, app)
	}

