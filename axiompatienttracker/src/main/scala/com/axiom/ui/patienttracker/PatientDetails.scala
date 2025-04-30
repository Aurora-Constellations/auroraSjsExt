package com.axiom.ui.patienttracker

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import com.axiom.ModelFetch
import scala.concurrent.ExecutionContext.Implicits.global

def renderPatientDetailsPage(unitNumber: String): Unit = {
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
      val details = List(
        "Account Number" -> patient.accountNumber,
        "Unit Number" -> patient.unitNumber,
        "Last Name" -> patient.lastName,
        "First Name" -> patient.firstName,
        "Gender" -> patient.sex,
        "Date of Birth" -> patient.dob.map(_.toString).getOrElse(""),
        "Health Card Number" -> patient.hcn.getOrElse(""),
        "Admit Date" -> patient.admitDate.map(_.toString).getOrElse(""),
        "Floor" -> patient.floor.getOrElse(""),
        "Room" -> patient.room.getOrElse(""),
        "Bed" -> patient.bed.getOrElse(""),
        "MRP" -> patient.mrp.getOrElse(""),
        "Admitting Physician" -> patient.admittingPhys.getOrElse(""),
        "Family" -> patient.family.getOrElse(""),
        "Family Privacy" -> patient.famPriv.getOrElse(""),
        "Hospital" -> patient.hosp.getOrElse(""),
        "Flag" -> patient.flag.getOrElse(""),
        "Service" -> patient.service.getOrElse(""),
        "Address 1" -> patient.address1.getOrElse(""),
        "Address 2" -> patient.address2.getOrElse(""),
        "City" -> patient.city.getOrElse(""),
        "Province" -> patient.province.getOrElse(""),
        "Postal Code" -> patient.postalCode.getOrElse(""),
        "Home Phone Number" -> patient.homePhoneNumber.getOrElse(""),
        "Work Phone Number" -> patient.workPhoneNumber.getOrElse(""),
        "OHIP" -> patient.ohip.getOrElse(""),
        "Attending" -> patient.attending.getOrElse(""),
        "Collaborator 1" -> patient.collab1.getOrElse(""),
        "Collaborator 2" -> patient.collab2.getOrElse(""),
        "Aurora File" -> patient.auroraFile.getOrElse("")
      ).filterNot { case (key, value) => value.isEmpty && key != "Aurora File" }

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
                renderDetails("details-section", "Personal Information", details, Set("Account Number", "Unit Number", "Last Name", "First Name", "Gender", "Date of Birth", "Health Card Number")),
                renderDetails("details-section", "Admission Details", details, Set("Admit Date", "Floor", "Room", "Bed", "MRP", "Admitting Physician")),
                renderDetails("details-section", "Contact Information", details, Set("Address 1", "Address 2", "City", "Province", "Postal Code", "Home Phone Number", "Work Phone Number")),
                renderDetails("details-section", "Other Details", details, Set("Hospital", "Flag", "Service", "OHIP", "Attending", "Collaborator 1", "Collaborator 2", "Aurora File", "Family", "Family Privacy"))
            )
          )
        ),
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

private def renderDetails(cssClass:String, heading:String, details: List[(String, String)], fields: Set[String]) = {
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
                  println("Creating new Aurora File")
                  // TODO: Add logic to create a new file
                }
              )
            } else {
              button(
                cls := "open-button",
                "Open File",
                onClick --> { _ =>
                  println(s"Opening the Aurora File: $fieldValue")
                  // TODO: Add logic to open aurora file
                }
              )
            }
          )
        } 
        else{
          li(
            span(cls := "field-name", s"$fieldName: "),
            span(cls := "field-value", fieldValue)
          )
        }
      }
    )
  )
  section
}