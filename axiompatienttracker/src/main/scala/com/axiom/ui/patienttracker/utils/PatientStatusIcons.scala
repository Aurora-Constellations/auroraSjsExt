package com.axiom.ui.patienttracker.utils

import com.raquo.laminar.api.L.{*, given}

enum PatientStatus:
  case Stable, Urgency, Draft, Combined

object PatientStatus:
  def fromString(code: String): PatientStatus = code match
    case "0"  => Stable
    case "1"  => Urgency
    case "2"  => Draft
    case "12" => Combined
    case _    => Stable  // Default fallback (optional)
//Helper function to render stauts Icons
object PatientStatusIcons {
  def renderStatusIcon(status: PatientStatus): HtmlElement = status match
    case PatientStatus.Stable =>
      img(
        cls := "status-column",
        src := "https://img.icons8.com/material-rounded/24/40C057/checked-checkbox.png",
        alt := "Stable",
        width := "20px",
        height := "20px"
      )

    case PatientStatus.Urgency =>
      img(
        src := "https://img.icons8.com/fluency-systems-filled/48/FA5252/leave.png",
        alt := "Urgency",
        width := "20px",
        height := "20px"
      )

    case PatientStatus.Draft =>
      img(
        src := "https://img.icons8.com/ios-filled/50/FAB005/create-new.png",
        alt := "Draft",
        width := "20px",
        height := "20px"
      )

    case PatientStatus.Combined =>
      div(
        cls := "status-column",
        renderStatusIcon(PatientStatus.Urgency),
        renderStatusIcon(PatientStatus.Draft)
      )
}
