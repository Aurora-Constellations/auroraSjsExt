
package com.axiom.ui.patienttracker.utils

import com.raquo.laminar.api.L._
import org.scalajs.dom

sealed trait Urgent
case object Urgent extends Urgent

sealed trait Draft
case object Draft extends Draft

final case class Status(urgent: Option[Urgent], draft: Option[Draft]) {
  def isUrgent: Boolean = urgent.nonEmpty
  def isDraft:  Boolean = draft.nonEmpty
  def isStable: Boolean = !isUrgent && !isDraft
}

object Status {
  
  val Stable: Status   = Status(None, None)
  val OnlyUrgent: Status = Status(Some(Urgent), None)
  val OnlyDraft: Status  = Status(None, Some(Draft))
  val Combined: Status   = Status(Some(Urgent), Some(Draft))

 
  def fromCode(code: String): Status = code match {
    case "*" | "1"  => OnlyUrgent
    case "2"  => OnlyDraft
    case "12" => Combined
    case "0"  => Stable
    case _    => Stable // fallback
  }

  
  def toCode(s: Status): String = s match {
    case Status(None, None)       => "0"
    case Status(Some(_), None)    => "1"
    case Status(None, Some(_))    => "2"
    case Status(Some(_), Some(_)) => "12"
  }
}

object StatusIcons {
 
  //  URLs
  private object Urls {
    val urgent = "https://img.icons8.com/fluency-systems-filled/48/FA5252/leave.png"
    val draft  = "https://img.icons8.com/ios-filled/50/FAB005/create-new.png"
    val stable = "https://img.icons8.com/material-rounded/24/40C057/checked-checkbox.png"
  }

  /** Build a NEW <img> each time we call this. */
  private def icon(srcUrl: String, altText: String): HtmlElement = {
    // optional: fallback if the host blocks hotlinking
    val srcVar = Var(srcUrl)
    img(
      cls := "status-icon",
      src <-- srcVar.signal,
      alt := altText,
      width := "18",    // use numbers for HTML width/height
      height := "18",
      onError.mapTo(Urls.stable) --> srcVar.writer // fallback image
    )
  }

  
  private def urgentIcon(): HtmlElement = icon(Urls.urgent, "Urgent")
  private def draftIcon():  HtmlElement = icon(Urls.draft,  "Draft")
  private def stableIcon(): HtmlElement = icon(Urls.stable, "Stable")

  def render(status: Status): HtmlElement = {
    // decide which icons to show
    val icons =
      (if status.isUrgent then List(urgentIcon()) else Nil) :::
      (if status.isDraft  then List(draftIcon())  else Nil)

    
    val container = div(cls := "status-column")
    if icons.isEmpty then container.amend(stableIcon())
    else icons.foreach(container.amend(_))
    container
  }
}

