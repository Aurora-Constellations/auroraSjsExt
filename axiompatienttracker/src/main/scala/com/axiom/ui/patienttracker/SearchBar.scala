package com.axiom.ui.patienttracker

import com.raquo.laminar.api.L._
import org.scalajs.dom

object SearchBar {
  def apply(searchQueryVar: Var[String], onCreatePatient: () => Unit): HtmlElement = {
    div(
      cls := "search-bar",
      // First span with the "Patient list" title
      span(cls := "patient-title", "Patient list"),// First title span
      // Search bar
      label("Search: "),
      marginBottom := "10px",
      input(
        typ := "text",
        placeholder := "Search patients here...",
        inContext { thisNode =>
          onInput.mapTo(thisNode.ref.value) --> searchQueryVar
        }
      ),
      button(
        "Create Patient",
        cls := "create-patient-button",
        onClick --> { _ => 
          println("Create Patient button clicked")
          onCreatePatient()
        }
      )
    )
  }
}
