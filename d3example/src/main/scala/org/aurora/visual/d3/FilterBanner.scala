package org.aurora.visual.d3

import scala.scalajs.js
import org.scalajs.dom

// Banner over the diagram while Hide Narratives or Hide Named Groups is hiding something, so a filtered diagram is
// never mistaken for the whole picture. "Show all" asks the extension to clear the filters. Shared by both renderers.
// before: place the banner in the page flow just before this element (the end-user diagram, which then shrinks and
// re-fits around it); otherwise it is pinned to the top of the window over the developer diagram
class FilterBanner(parent: dom.Element, before: Option[dom.Element] = None):

    private val banner = dom.document.createElement("div").asInstanceOf[dom.html.Div]
    banner.setAttribute("class", if (before.isDefined) "aurora-filter-banner is-inline" else "aurora-filter-banner")
    banner.setAttribute("role", "status")
    banner.setAttribute("aria-live", "polite")
    banner.setAttribute("hidden", "")

    private val icon = dom.document.createElement("span")
    icon.setAttribute("class", "aurora-filter-banner-icon")
    icon.setAttribute("aria-hidden", "true")
    // Crossed-out eye
    icon.innerHTML =
        """<svg viewBox="0 0 16 16"><path d="M2 8s2.2-4 6-4 6 4 6 4-2.2 4-6 4-6-4-6-4z"/><circle cx="8" cy="8" r="1.8"/><path d="M3 13L13 3"/></svg>"""

    private val text = dom.document.createElement("span")

    private val showAll = dom.document.createElement("button").asInstanceOf[dom.html.Button]
    showAll.setAttribute("class", "aurora-filter-banner-action")
    showAll.textContent = "Show all"
    showAll.addEventListener(
        "click",
        (_: dom.Event) => VsCodeApi.api.foreach(_.postMessage(js.Dynamic.literal(command = "showAll")))
    )

    banner.appendChild(icon)
    banner.appendChild(text)
    banner.appendChild(showAll)
    before match {
        case Some(next) => parent.insertBefore(banner, next)
        case None       => parent.appendChild(banner)
    }

    // None hides the banner
    def show(message: Option[String]): Unit = message match {
        case Some(m) =>
            text.textContent = m
            banner.removeAttribute("hidden")
        case None =>
            banner.setAttribute("hidden", "")
    }
