package com.axiom.ui.patienttracker

import com.raquo.laminar.api.L.{*, given}

//Helper function to render stauts Icons
object PatientStatusIcons {
  def renderStatusIcon(status: String): HtmlElement = status match
    case "0" =>
      img(src := "https://img.icons8.com/material-rounded/24/40C057/checked-checkbox.png", 
      alt := "Stable", 
      width := "20px", 
      height := "20px")
    case "1" =>
      img(src := "https://img.icons8.com/fluency-systems-filled/48/FA5252/leave.png", 
      alt := "Urgency", 
      width := "20px", 
      height := "20px")
    case "2" =>
      img(src := "https://img.icons8.com/ios-filled/50/FAB005/create-new.png", 
      alt := "Draft", 
      width := "20px", 
      height := "20px")
    case "12" =>
      div(
        cls := "status-column",
        renderStatusIcon("1"),
        renderStatusIcon("2")
      )
    case _ =>
      img(src := "https://img.icons8.com/material-rounded/24/40C057/checked-checkbox.png", 
      alt := "Stable", 
      width := "20px", 
      height := "20px")
}
