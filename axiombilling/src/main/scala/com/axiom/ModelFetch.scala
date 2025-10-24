package com.axiom

import io.laminext.fetch._
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import zio.json._
import org.scalajs.dom.AbortController
import com.axiom.model.shared.dto.{Patient, Account, Encounter, Billing}

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
