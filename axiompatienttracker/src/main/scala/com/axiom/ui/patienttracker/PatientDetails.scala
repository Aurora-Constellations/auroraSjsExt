package com.axiom.ui.patienttracker

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import com.axiom.ModelFetch
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}
import com.axiom.model.shared.dto.Patient
import java.time.{LocalDate, LocalDateTime}
import com.axiom.messaging.*
import com.axiom.ui.patienttracker.DataProcessing._


//Added case class to tag editable values
case class PatientFormState (
    firstName: Var[String],
    lastName: Var[String],
    dob: Var[String],
    sex: Var[String],
    hcn: Var[String],
    family: Var[String],
    famPriv: Var[String],
    service: Var[String],
    attending: Var[String],
    auroraFile: Var[String]
)
def renderPatientDetailsPage(unitNumber: String, editable: Boolean = false): Unit = {
  println(s"Fetching details for unit number: $unitNumber")
  val container = dom.document.getElementById("app")
  if (container != null) {
    container.innerHTML = "" // Clear only the container
  } else {
    println("Error: Container element not found!")
  }

  // Fetch patient details using the unit number
  ModelFetch.fetchPatientDetails(unitNumber).map {
    case Some(patient) =>
      val details = extractPatientDetails(patient)

      //The fields which can be edited
      val formState = createPatientFormState(patient)

      val detailsPage = div(
        cls := "patient-details-page",
        h1("Patient Details"),
        div(
          cls := "patient-details-container",
          div(
            cls := "patient-image-box",
            div(
              cls := "patient-image",
              img(
                src := "https://robohash.org/mail@ashallendesign.co.uk",
                alt := "Patient Image"
              )
            )
          ),
          div(
            cls := "patient-info-box",
            div(
              cls := "patient-info",
                renderDetails("details-section", "Personal Information", details, Set("Account Number", "Unit Number", "Last Name", "First Name", "Gender", "Date of Birth", "Health Card Number"),editable,Some(formState)),
                renderDetails("details-section", "Admission Details", details, Set("Admit Date", "Floor", "Room", "Bed", "MRP", "Admitting Physician"),editable,Some(formState)),
                renderDetails("details-section", "Contact Information", details, Set("Address 1", "Address 2", "City", "Province", "Postal Code", "Home Phone Number", "Work Phone Number"),editable,Some(formState)),
                renderDetails("details-section", "Other Details", details, Set("Hospital", "Flag", "Service", "OHIP", "Attending", "Collaborator 1", "Collaborator 2", "Aurora File", "Family", "Family Privacy"),editable,Some(formState))
            )
          )
        ),
        div(
          // adding save changes button only if the editable flag is true
          if (editable) { 
            button(
              "Save Changes",
              cls := "save-button",
              onClick --> { _ =>
                println("Save button clicked")

                //a new Patient object is created from form values
                val updatedPatient = buildUpdatedPatient(patient, formState)


                // Call the backend update API by passing the updated values
                ModelFetch.updatePatientDetails(unitNumber, updatedPatient).foreach {
                  case Some(updated) =>
                    println(s"Patient updated successfully: ${updated.unitNumber}")
                    val tracker = new PatientTracker()
                    ModelFetch.fetchPatients.foreach { patients =>
                      tracker.populate(patients)
                    }
                    container.innerHTML = ""
                    render(container, tracker.renderHtml)
                  case None =>
                    println("Failed to update patient.")
                  }
              }
            )
          }else emptyNode ,
          
          button(
            "Back to List",
            cls := "back-button",
            width:= "100%",
            onClick --> { _ =>
              println("Back to list clicked")
              val patientTracker = new PatientTracker()
              ModelFetch.fetchPatients.foreach{ p => 
                patientTracker.populate(p)
              }
              container.innerHTML = "" // Clear only the container
              render(container, patientTracker.renderHtml)
            }
          )
        )
      )
      render(container, detailsPage)

    case None =>
      println(s"No details found for unit number: $unitNumber")
      val errorPage = div(
        cls := "error-page",
        div(
          cls := "error-container",
          h1(cls := "error-title", "Error"),
          p(cls := "error-message", s"No patient details found for unit number: $unitNumber"),
          div(
            cls := "back-to-list",
            button(
              "Back to List",
              cls := "back-button",
              width:= "100%",
              onClick --> { _ =>
                println("Back to list clicked")
                val patientTracker = new PatientTracker()
                ModelFetch.fetchPatients.foreach{ p => 
                  patientTracker.populate(p)
                }
                container.innerHTML = "" // Clear only the container
                render(container, patientTracker.renderHtml)
              }
            )
          )
        )
      )
      render(container, errorPage)
  }
}

private def renderDetails(cssClass:String, heading:String, details: List[(String, String)], fields: Set[String], editable: Boolean, formState: Option[PatientFormState]) = {
  val unitNumber = details(1)._2
  
  def renderEditableField(fieldName: String, fieldValue: String): HtmlElement = fieldName match {
    case "First Name"         => input(value := formState.get.firstName.now(), onInput.mapToValue --> formState.get.firstName)
    case "Last Name"          => input(value := formState.get.lastName.now(), onInput.mapToValue --> formState.get.lastName)
    case "Gender" | "Sex"     => input(value := formState.get.sex.now(), onInput.mapToValue --> formState.get.sex)
    case "Date of Birth"      => input(value := formState.get.dob.now(), onInput.mapToValue --> formState.get.dob)
    case "Health Card Number" => input(value := formState.get.hcn.now(), onInput.mapToValue --> formState.get.hcn)
    case "Family"             => input(value := formState.get.family.now(), onInput.mapToValue --> formState.get.family)
    case "Family Privacy"     => input(value := formState.get.famPriv.now(), onInput.mapToValue --> formState.get.famPriv)
    case "Service"            => input(value := formState.get.service.now(), onInput.mapToValue --> formState.get.service)
    case "Attending"          => input(value := formState.get.attending.now(), onInput.mapToValue --> formState.get.attending)
    case "Aurora File"        => input(value := formState.get.auroraFile.now(), onInput.mapToValue --> formState.get.auroraFile)
    case _                    => span(cls := "field-value", fieldValue)
  }

  val section = div(
    cls := cssClass,
    h2(heading),
    ul(
      details.filter { case (fieldName, _) => fields.contains(fieldName) }
      .map { case (fieldName, fieldValue) =>
        if (fieldName == "Aurora File") {
          li(
            span(cls := "field-name", s"$fieldName: "),
            if (fieldValue.isEmpty) {
              button(
                cls := "create-button",
                "Create File",
                onClick --> { _ =>
                  println(s"Creating new Aurora File: ${unitNumber}")
                  sendRequestToVSCode(Request("createAuroraFile", CreateAuroraFile(s"$unitNumber.aurora")))
                  // Add the aurora file to database
                  ModelFetch.addPatientAuroraFile(unitNumber).map {
                    case Some(_) =>
                      println(s"Aurora file created successfully: ${unitNumber}.aurora")
                    case None => 
                      println(s"Failed to create Aurora file for unit number: ${unitNumber}")
                  }
                  sendResponseToVSCode(Response("addedToDB", AddedToDB(s"$unitNumber.aurora")))
                }
              )
            } else {
              button(
                cls := "open-button",
                "Open File",
                onClick --> { _ =>
                  println(s"Opening the Aurora File: $fieldValue")
                  sendRequestToVSCode(Request("openAuroraFile", OpenAuroraFile(s"$unitNumber.aurora")))
                }
              )
            }
          )
        } 
        else{
          li(
            //Rendering inputs conditionally. 
            span(cls := "field-name", s"$fieldName: "),
            //if editable inputs are added 
             if (editable) {
                renderEditableField(fieldName, fieldValue)
              } else {
                span(cls := "field-value", fieldValue)
              }
          )
        }
      }
    )
  )
  section
}