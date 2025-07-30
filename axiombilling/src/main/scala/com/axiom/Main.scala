package com.axiom


import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

object Main :
	def consoleOut(msg: String): Unit = {
		dom.console.log(s"%c $msg","background: #222; color: #bada55")
	}

	@main def entrypoint(): Unit = 
		consoleOut ("Hello, Billing from console!")
		println("Hello, Billing!")
		// Scala.js outputs to the browser dev console, not the sbt session
		// Always have the browser dev console open when developing web UIs.

		val element = dom.document.querySelector("#app")
		element match
			case null => consoleOut("No element with id 'app' found in the document.")
			case el: dom.html.Element =>
				val app = div(
					h1("Axiom Billing"),
					p("This is a simple Billing application built with Scala.js and Laminar."),
					button(onClick --> { _ => consoleOut("Button clicked!") }, "Click Me")
				)
				render(el, app)
				consoleOut("App rendered successfully.")
