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


  
  def createPatientForm(state: PatientTracker#FormState, showVar: Var[Boolean], onClose: () => Unit): HtmlElement =
    val errorVars = Map(
      "firstName" -> Var(Option.empty[String]),
      "lastName" -> Var(Option.empty[String]),
      "unitNumber" -> Var(Option.empty[String]),
      "accountNumber" -> Var(Option.empty[String]),
      "sex" -> Var(Option.empty[String]),
      "dob" -> Var(Option.empty[String]),
      "admitDate" -> Var(Option.empty[String]),
      "hosp" -> Var(Option.empty[String])
    )

    def validateField(fieldName: String, value: String): Boolean =
      if (value.trim.isEmpty) {
        errorVars(fieldName).set(Some("This field is required."))
        false
      } else {
        errorVars(fieldName).set(None)
        true
      }

    def validateForm(): Boolean =
      val results = Seq(
        "firstName" -> state.firstNameVar.now(),
        "lastName" -> state.lastNameVar.now(),
        "unitNumber" -> state.unitNumberVar.now(),
        "accountNumber" -> state.accountNumberVar.now(),
        "sex" -> state.sexVar.now(),
        "dob" -> state.dobVar.now(),
        "admitDate" -> state.admitDateVar.now(),
        "hosp" -> state.hospVar.now()
      ).map { case (key, value) => validateField(key, value) }
      results.forall(_ == true)

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
                if validateForm() then
                  val admitDateRaw = state.admitDateVar.now()
                  val formattedAdmitDate =
                    if (admitDateRaw.length == 16) s"$admitDateRaw:00" else admitDateRaw

                  val patient = Patient(
                    accountNumber = state.accountNumberVar.now(),
                    unitNumber = state.unitNumberVar.now(),
                    lastName = state.lastNameVar.now(),
                    firstName = state.firstNameVar.now(),
                    sex = state.sexVar.now(),
                    dob = Some(LocalDate.parse(state.dobVar.now())),
                    hcn = None,
                    admitDate = Some(LocalDateTime.parse(formattedAdmitDate)),
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
                else
                  println("Validation failed.")
              },
              Seq(
                "First Name*" -> ("firstName", state.firstNameVar),
                "Last Name*" -> ("lastName", state.lastNameVar),
                "Unit Number*" -> ("unitNumber", state.unitNumberVar),
                "Account Number*" -> ("accountNumber", state.accountNumberVar),
                "Gender*" -> ("sex", state.sexVar),
                "DOB (yyyy-MM-dd)*" -> ("dob", state.dobVar),
                "Admit Date (yyyy-MM-ddTHH:mm:ss)*" -> ("admitDate", state.admitDateVar),
                "Hospital*" -> ("hosp", state.hospVar),
                "Floor" -> ("", state.floorVar),
                "Room" -> ("", state.roomVar),
                "Bed" -> ("", state.bedVar)
              ).map { case (labelText, (key, varRef)) =>
                val errorSignal = errorVars.get(key).map(_.signal).getOrElse(Val(None))
                val (fieldLabel, isRequired) =
                if labelText.endsWith("*") then (labelText.dropRight(1).trim, true)
                else (labelText, false)
                            div(
                              marginBottom := "12px",
                              label(
                  span(fieldLabel),
                  if isRequired then span("*", color := "red", fontWeight.bold) else emptyNode,
                  display.block,
                  marginBottom := "4px"
                ),
                input(
                  typ := "text",
                  className := "input", // keep it simple
                  onInput.mapToValue --> varRef
                ),
                  child.maybe <-- errorSignal.map {
                    case Some(msg) => Some(span(msg, cls := "error-text"))
                    case None      => None
                  }
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

    
    //Helper function to render "View Details" and "Edit" actions on the patient tracker

  def renderActionButtons(unitNumber: String): HtmlElement =
    td(
      cls := "details-column",
      button(
        "View Details", 
        marginRight := "8px", 
        onClick --> { _ => 
          renderPatientDetailsPage(unitNumber) }),
      button(
        "Edit", 
        onClick --> { _ => 
          renderPatientDetailsPage(unitNumber, editable = true) 
          }
          )
    )