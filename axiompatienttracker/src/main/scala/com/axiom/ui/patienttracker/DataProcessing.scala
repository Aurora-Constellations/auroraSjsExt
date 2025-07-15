package com.axiom.ui.patienttracker

import java.time.{LocalDate, LocalDateTime}
import com.axiom.model.shared.dto.Patient
import com.raquo.laminar.api.L._

object DataProcessing:

  val errorVars: Map[String, Var[Option[String]]] = Map(
    "firstName"     -> Var(None),
    "lastName"      -> Var(None),
    "unitNumber"    -> Var(None),
    "accountNumber" -> Var(None),
    "sex"           -> Var(None),
    "dob"           -> Var(None),
    "admitDate"     -> Var(None),
    "hosp"          -> Var(None)
  )

  def validateField(fieldName: String, value: String): Boolean =
    if value.trim.isEmpty then
      errorVars(fieldName).set(Some("This field is required."))
      false
    else
      errorVars(fieldName).set(None)
      true

  def validateForm(state: PatientTracker#FormState): Boolean =
    val fields = Seq(
      "firstName"     -> state.firstNameVar.now(),
      "lastName"      -> state.lastNameVar.now(),
      "unitNumber"    -> state.unitNumberVar.now(),
      "accountNumber" -> state.accountNumberVar.now(),
      "sex"           -> state.sexVar.now(),
      "dob"           -> state.dobVar.now(),
      "admitDate"     -> state.admitDateVar.now(),
      "hosp"          -> state.hospVar.now()
    )
    fields.map((validateField _).tupled).forall(_ == true)

  def getErrorSignal(key: String): Signal[Option[String]] =
    errorVars.getOrElse(key, Var(None)).signal

def createPatientFormState(patient: Patient): PatientFormState =
  PatientFormState(
    firstName   = Var(patient.firstName),
    lastName    = Var(patient.lastName),
    dob         = Var(patient.dob.map(_.toString).getOrElse("")),
    sex         = Var(patient.sex),
    hcn         = Var(patient.hcn.getOrElse("")),
    family      = Var(patient.family.getOrElse("")),
    famPriv     = Var(patient.famPriv.getOrElse("")),
    service     = Var(patient.service.getOrElse("")),
    attending   = Var(patient.attending.getOrElse("")),
    auroraFile  = Var(patient.auroraFile.getOrElse(""))
  )


def buildPatientFromState(state: PatientTracker#FormState): Patient =
    val admitDateRaw = state.admitDateVar.now()
    val formattedAdmitDate =
      if admitDateRaw.length == 16 then s"$admitDateRaw:00" else admitDateRaw

    Patient(
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

def extractPatientDetails(patient: Patient): List[(String, String)] =
  List(
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


def buildUpdatedPatient(original: Patient, formState: PatientFormState): Patient =
  original.copy(
    firstName = formState.firstName.now(),
    lastName = formState.lastName.now(),
    sex = formState.sex.now(),
    dob = Some(LocalDate.parse(formState.dob.now())),
    hcn = Some(formState.hcn.now()),
    family = Some(formState.family.now()),
    famPriv = Some(formState.famPriv.now()),
    service = Some(formState.service.now()),
    attending = Some(formState.attending.now()),
    auroraFile = Some(formState.auroraFile.now())
  )

