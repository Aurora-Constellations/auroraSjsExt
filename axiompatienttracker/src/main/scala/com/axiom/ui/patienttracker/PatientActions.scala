package com.axiom.ui.patienttracker

// this file handles all the patient tracker actions like create,viewing, navigations of the page, edit, delete etc 

import scala.scalajs.js
import com.axiom.ModelFetch
import com.raquo.laminar.api.L._
import org.scalajs.dom
import java.time.{LocalDate, LocalDateTime}
import com.axiom.model.shared.dto.Patient
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global



object PatientActions:


  
  def createPatientForm(state: PatientTracker#FormState,showVar: Var[Boolean], onClose: () => Unit): HtmlElement =
    div(
      className := "create-patient-modal-overlay",
      display <-- showVar.signal.map {
        case true  => "flex"
        case false => "none"
        },

      child <-- showVar.signal.map {
        case true =>
          div(
            className := "create-patient-modal",
            h2("Create New Patient"),
            form(
              onSubmit.preventDefault --> { _ =>
                // 🔧 Fix for parsing admit date safely
                val admitDateRaw = state.admitDateVar.now()
                val formattedAdmitDate =
                  if (admitDateRaw.length == 16) s"$admitDateRaw:00" else admitDateRaw
                val admitDateOpt = Some(LocalDateTime.parse(formattedAdmitDate))
                println(s"Formatted Admit Date: $formattedAdmitDate")

                val patient = Patient(
                  accountNumber = state.accountNumberVar.now(),
                  unitNumber = state.unitNumberVar.now(),
                  lastName = state.lastNameVar.now(),
                  firstName = state.firstNameVar.now(),
                  sex = state.sexVar.now(),
                  dob = Some(LocalDate.parse(state.dobVar.now())),
                  hcn = None,
                  admitDate = Some(LocalDateTime.parse(state.admitDateVar.now())),
                  floor = Some(state.floorVar.now()),
                  room = Some(state.roomVar.now()),
                  bed = Some(state.bedVar.now()),
                  mrp = None,
                  admittingPhys = None,
                  family = None,
                  famPriv = None,
                  hosp = Some(state.hospVar.now()),
                  flag = None,
                  service = None,
                  address1 = None,
                  address2 = None,
                  city = None,
                  province = None,
                  postalCode = None,
                  homePhoneNumber = None,
                  workPhoneNumber = None,
                  ohip = None,
                  attending = None,
                  collab1 = None,
                  collab2 = None,
                  auroraFile = None
                )
                ModelFetch.createPatient(patient).onComplete {
                  case Success(_) =>
                    println("Patient created successfully, reloading tracker")
                    val container = dom.document.getElementById("app")
                    if (container != null) {
                      container.innerHTML = ""
                      val tracker = new PatientTracker()
                      ModelFetch.fetchPatients.foreach { patients =>
                        tracker.populate(patients)
                        render(container, tracker.renderHtml)
                      }
                    }
                    onClose()
                    
                  case Failure(ex) =>
                    println(s"Failed to create patient: ${ex.getMessage}")
                }
              },
              Seq(
                "First Name" -> state.firstNameVar,
                "Last Name" -> state.lastNameVar,
                "Unit Number" -> state.unitNumberVar,
                "Account Number" -> state.accountNumberVar,
                "Gender" -> state.sexVar,
                "DOB (yyyy-MM-dd)" -> state.dobVar,
                "Admit Date (yyyy-MM-ddTHH:mm:ss)" -> state.admitDateVar,
                "Floor" -> state.floorVar,
                "Room" -> state.roomVar,
                "Bed" -> state.bedVar,
                "Hospital" -> state.hospVar
              ).map { case (labelText, varRef) =>
                div(
                  marginBottom := "12px",
                  label(labelText, display.block, marginBottom := "4px"),
                  input(
                    typ := "text",
                    className := "input",
                    onInput.mapToValue --> varRef
                  )
                )
              },
              div(
                className := "create-patient-modal-buttons",
                button("Submit", className := "submit-btn"),
                button("Cancel", className := "cancel-btn", onClick --> (_ => onClose()))
              )
            )
          )
        case false => emptyNode
      }
    )
  