package com.axiom

import io.laminext.fetch._
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import zio.json._
import org.scalajs.dom.AbortController
import com.axiom.model.shared.dto.{Patient, Account, Encounter, Billing}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.scalajs.dom
import org.scalajs.dom.experimental.{Headers => DomHeaders, RequestInit, HttpMethod}

object ModelFetch:

  val abortController = new AbortController()

  // --- PATIENTS ---
  def fetchPatients: Future[List[Patient]] =
    Fetch.get("http://localhost:8080/patients")
      .future.text(abortController)
      .map(r => r.data.fromJson[List[Patient]].getOrElse(Nil))

  // --- ACCOUNTS BY PATIENT ---
  def fetchAccountsByPatientId(patientId: Long): Future[List[Account]] =
    Fetch.get(s"http://localhost:8080/account/patient/$patientId")
      .future.text(abortController)
      .map(r => r.data.fromJson[List[Account]].getOrElse(Nil))

  // --- ENCOUNTERS BY ACCOUNT ---
  def fetchEncountersByAccountId(accountId: Long): Future[List[Encounter]] =
    Fetch.get(s"http://localhost:8080/encounters/account/$accountId")
      .future.text(abortController)
      .map(r => r.data.fromJson[List[Encounter]].getOrElse(Nil))

  // --- BILLINGS (ALL) ---
  def fetchAllBillings: Future[List[Billing]] =
    Fetch.get("http://localhost:8080/billings")
      .future.text(abortController)
      .map(r => r.data.fromJson[List[Billing]].getOrElse(Nil))

  final case class CreateAccountRequest(patientId: Long, startDate: String)
  object CreateAccountRequest:
    given zio.json.JsonEncoder[CreateAccountRequest] = zio.json.DeriveJsonEncoder.gen


  def createAccount(patientId: Long, start: LocalDateTime): Future[Option[Account]] =
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm") 
    val payload   = CreateAccountRequest(patientId, formatter.format(start)).toJson

    val httpheaders = new DomHeaders()
    httpheaders.set("Content-Type", "application/json")

    val req = new RequestInit:
      method = HttpMethod.POST
      body   = payload
      this.headers = httpheaders

    dom.experimental.Fetch
      .fetch("http://localhost:8080/account", req)
      .toFuture
      .flatMap(_.text().toFuture)                  // read response body as text
      .map(_.fromJson[Account].toOption)           // try decode as Account
      .recover { case ex =>
        dom.console.error(s"[createAccount] failed: ${ex.getMessage}")
        None
      }


  final case class CreateEncounterRequest(
  accountId: Long,
  doctorId: Long,
  startDate: String,
  auroraFileContent: List[Int]
)
  object CreateEncounterRequest:
    given JsonEncoder[CreateEncounterRequest] = DeriveJsonEncoder.gen

  def createEncounter(
    accountId: Long,
    doctorId: Long,
    start: LocalDateTime,
    auroraFileContent: List[Int] = Nil // can default to empty if needed
  ): Future[Option[Encounter]] =
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    val payload   = CreateEncounterRequest(accountId, doctorId, formatter.format(start), auroraFileContent).toJson

    val httpHeaders = new DomHeaders()
    httpHeaders.set("Content-Type", "application/json")

    val req = new RequestInit:
      method = HttpMethod.POST
      body   = payload
      this.headers = httpHeaders

    dom.experimental.Fetch
      .fetch("http://localhost:8080/encounter", req)
      .toFuture
      .flatMap(_.text().toFuture)
      .map(_.fromJson[Encounter].toOption)
      .recover { case ex =>
        dom.console.error(s"[createEncounter] failed: ${ex.getMessage}")
        None
      }