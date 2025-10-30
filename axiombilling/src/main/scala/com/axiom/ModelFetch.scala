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
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm") // "2023-11-24T00:00"
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