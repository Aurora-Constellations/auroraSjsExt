package com.axiom
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import com.axiom.model.shared.dto.Patient
import com.axiom.TableColProperties
import io.laminext.fetch._

import org.scalajs.dom.AbortController
import com.axiom.ShapelessFieldNameExtractor.fieldNames

import com.raquo.airstream.ownership.OneTimeOwner
  
object ModelFetch :
  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
  import zio.json._

  import io.laminext.fetch._
  import scala.concurrent.{Future,Promise}
  import org.scalajs.dom.AbortController
  import scala.collection.mutable

  val abortController = new AbortController()

  val columnHeaders =  
    val l = mutable.IndexedSeq(ShapelessFieldNameExtractor.fieldNames[Patient]*)
    l(0) = "*column 0*" 
    l.toList

  
  def columns(p:Patient) =  
    val c = mutable.IndexedSeq(TableColProperties.derived[Patient].element(p)*)
    c(0) = c(0).copy(text = s"*${c(0).text}*", color = "green")
    c.toList

  def fetchPatients = 
    import java.time._ //cross scalajs and jvm compatible
    import com.axiom.model.shared.dto.Patient 
    import com.axiom.ShapelessFieldNameExtractor
    
    Fetch.get("http://localhost:8080/patients").future.text(abortController)
      .map(s => s.data.fromJson[List[Patient]])
      .map(r => r.toOption.getOrElse(Nil))

  def fetchPatientDetails(unitNumber: String): Future[Option[Patient]] =
    Fetch.get(s"http://localhost:8080/patients/$unitNumber").future.text(abortController)
      .map(response => response.data.fromJson[Patient].toOption)
      .recover {
        case ex =>
          println(s"Error fetching patient details for unit number $unitNumber: ${ex.getMessage}")
          None
      }
    
  def addPatientAuroraFile(unitNumber: String): Future[Option[Patient]] =
    val auroraFile = s"""{"auroraFile": "$unitNumber.aurora"}"""
    Fetch.put(s"http://localhost:8080/patients/update/$unitNumber", auroraFile).future.text(abortController)
      .map(response => response.data.fromJson[Patient].toOption)
      .recover {
        case ex =>
          println(s"Error adding patient aurora file for unit number $unitNumber: ${ex.getMessage}")
          None
      }
  
  def addNarrativesFlag(unitNumber: String, flag: String): Future[Option[Patient]] =
    val narrativeFlag = s"""{"flag": "$flag"}"""
    Fetch.put(s"http://localhost:8080/patients/update/$unitNumber", narrativeFlag).future.text(abortController)
      .map(response => response.data.fromJson[Patient].toOption)
      .recover {
        case ex =>
          println(s"Error adding narrative flag for unit number $unitNumber: ${ex.getMessage}")
          None
      }

  def createPatient(patient: Patient): Future[Option[Patient]] = {
  import org.scalajs.dom.experimental.{Headers => DomHeaders, RequestInit, HttpMethod}
  import scala.scalajs.js
  import scala.scalajs.js.Thenable.Implicits._
  import zio.json._

  val jsonBody = patient.toJson

  // Helper to validate date fields
  def validateField[T](label: String, value: Option[T]): Boolean = {
    value match {
      case Some(v) =>
        println(s"$label: $v")
        true
      case None =>
        println(s"Invalid or missing $label!")
        false
    }
  }

  // Validate dates before sending
  val isValid = validateField("Date of Birth", patient.dob) &&
                validateField("Admission Date", patient.admitDate)

  if (!isValid) return Future.successful(None)

  // Set headers
  val httpHeaders = new DomHeaders()
  httpHeaders.set("Content-Type", "application/json")

  // Prepare request
  val reqInit = new RequestInit {
    method = HttpMethod.POST
    body = jsonBody
    headers = httpHeaders
  }

  // Execute POST request and decode JSON
  org.scalajs.dom.experimental.Fetch
    .fetch("http://localhost:8080/patients", reqInit)
    .toFuture
    .flatMap(_.text().toFuture)
    .map(_.fromJson[Patient].toOption)
    .recover {
      case ex =>
        println("Exception while creating patient:")
        println(s"Message: ${ex.getMessage}")
        ex.printStackTrace()
        None
    }
}
