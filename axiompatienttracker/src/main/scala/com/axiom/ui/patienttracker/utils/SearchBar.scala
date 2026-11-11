package com.axiom.ui.patienttracker.utils

import com.raquo.laminar.api.L._
import org.scalajs.dom

object SearchBar {
  def apply(searchQueryVar: Var[String], onCreatePatient: () => Unit): HtmlElement = {
    // Results filter as you type; the search button applies the current text and returns focus to the box
    val searchInput = input(
      typ := "text",
      idAttr := "patient-search",
      placeholder := "Search patients...",
      inContext { thisNode =>
        onInput.mapTo(thisNode.ref.value) --> searchQueryVar
      }
    )

    div(
      cls := "search-bar",
      // First span with the "Patient list" title
      span(cls := "patient-title", "Patient list"),// First title span
      // Search bar
      label("Search: ", forId := "patient-search"),
      marginBottom := "10px",
      div(
        cls := "search-field",
        searchInput,
        button(
          cls := "search-button",
          typ := "button",
          title := "Search",
          aria.label := "Search",
          onClick --> { _ =>
            searchQueryVar.set(searchInput.ref.value)
            searchInput.ref.focus()
          },
          // Magnifying glass
          svg.svg(
            svg.viewBox := "0 0 16 16",
            svg.className := "search-icon",
            svg.circle(svg.cx := "7", svg.cy := "7", svg.r := "4.5"),
            svg.path(svg.d := "M10.5 10.5L14 14")
          )
        )
      ),
      button(
        "Add Patient",
        cls := "create-patient-button",
        onClick --> { _ => 
          println("Add Patient button clicked")
          onCreatePatient()
        }
      )
    )
  }
}
