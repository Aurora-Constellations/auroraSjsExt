package com.axiom
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import com.axiom.model.shared.dto.Patient
import com.axiom.TableColProperties
//TODO[DELETE THIS]  this will be encapsulated in PatientTracker
object Model :
  type PatientFields = List[Display]

  val patientListVar = Var(List.empty[Patient])
  val colHeadersVar = Var(List.empty[String])
  val patientFieldEnums = Var(List[PatientFields]())

  def fetchPatients = ModelFetch.fetchPatients





