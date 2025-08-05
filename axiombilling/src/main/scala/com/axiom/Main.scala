package com.axiom


import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import com.axiom.ui.*
import java.time.LocalDate
import java.time.LocalDateTime
import com.axiom.model.shared.dto._

object Main :
	def consoleOut(msg: String): Unit = {
		dom.console.log(s"%c $msg","background: #222; color: #bada55")
	}

	@main def entrypoint(): Unit = {
		consoleOut("Starting app...")

		val patients = List(
			Patient(
				accountNumber = "1001",
				unitNumber = "U01",
				lastName = "Smith",
				firstName = "John",
				sex = "M",
				dob = Some(LocalDate.of(1985, 6, 15)),
				hcn = Some("HCN001"),
				admitDate = Some(LocalDateTime.of(2025, 7, 1, 10, 0)),
				floor = Some("2"),
				room = Some("202"),
				bed = Some("B"),
				mrp = Some("Dr. House"),
				admittingPhys = Some("Dr. Wilson"),
				family = Some("Yes"),
				famPriv = Some("No"),
				hosp = Some("GH"),
				flag = Some("None"),
				service = Some("Cardiology"),
				address1 = Some("123 Main St"),
				address2 = None,
				city = Some("Thunder Bay"),
				province = Some("ON"),
				postalCode = Some("P7B2R5"),
				homePhoneNumber = Some("807-111-2222"),
				workPhoneNumber = Some("807-333-4444"),
				ohip = Some("OH12345"),
				attending = Some("Dr. Strange"),
				collab1 = None,
				collab2 = None,
				auroraFile = None
			),
			Patient(
				accountNumber = "1002",
				unitNumber = "U02",
				lastName = "Doe",
				firstName = "Jane",
				sex = "F",
				dob = Some(LocalDate.of(1990, 3, 22)),
				hcn = Some("HCN002"),
				admitDate = Some(LocalDateTime.of(2025, 7, 5, 14, 30)),
				floor = Some("3"),
				room = Some("305"),
				bed = Some("A"),
				mrp = Some("Dr. Who"),
				admittingPhys = Some("Dr. Watson"),
				family = Some("No"),
				famPriv = Some("Yes"),
				hosp = Some("GH"),
				flag = Some("High Risk"),
				service = Some("Neurology"),
				address1 = Some("456 Elm St"),
				address2 = Some("Apt 2"),
				city = Some("Toronto"),
				province = Some("ON"),
				postalCode = Some("M5V2N8"),
				homePhoneNumber = Some("416-555-6666"),
				workPhoneNumber = None,
				ohip = Some("OH98765"),
				attending = Some("Dr. X"),
				collab1 = Some("Dr. Y"),
				collab2 = Some("Dr. Z"),
				auroraFile = None
			)
		)

		val accounts = List(
			Account(
				accountId = 1L,
				patientId = 1001L,
				startDate = LocalDateTime.of(2025, 7, 1, 10, 0),
				endDate = None // Active
			),
			Account(
				accountId = 2L,
				patientId = 1001L,
				startDate = LocalDateTime.of(2024, 1, 1, 9, 0),
				endDate = Some(LocalDateTime.of(2024, 2, 1, 12, 0))
			),
			Account(
				accountId = 3L,
				patientId = 1002L,
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
		patientsVar.set(patients)
		accountsVar.set(accounts)
		encountersVar.set(encounters)
		billingsVar.set(billings)

		val app = div(
			h2("Patient Billing Dashboard"),

			h3("Patients"),
			patientTable(patientsVar.signal),

			hr(),

			h3("Active Account"),
			activeAccountDisplay(activeAccountSignal),

			h3("Select Encounter"),
			encounterSelect(encounterOptionsSignal),

			h3("Billing Codes for Encounter"),
			billingList(billingForEncounterSignal)
		)

		dom.document.querySelector("#app") match
			case null => consoleOut("No element with id 'app' found.")
			case el: dom.html.Element => render(el, app)
	}

