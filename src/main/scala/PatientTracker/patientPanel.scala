package com.axiom.PatientTracker

import typings.vscode.mod as vscode
import vscode.{ExtensionContext}
import scala.scalajs.js
import com.axiom.patienttracker.PatientsListHtml.getPatientsListHtml
import com.axiom.WebviewMessageHandler.handleWebviewMessage
import com.axiom.messaging.*

private var patientsPanel: Option[vscode.WebviewPanel] = None // Store reference to the webview panel

def showPatients(context: vscode.ExtensionContext): js.Function1[Any, Any] =
	(_: Any) => {
	def revealOrCreate(): Unit =
		patientsPanel match {
		case Some(panel) if !js.isUndefined(panel) =>
			panel.reveal(vscode.ViewColumn.Two)
		case _ =>
			// Open webview beside, then move to bottom group
			createPatientsPanel(context, vscode.ViewColumn.Active)

			// Now move it to bottom group
			vscode.commands.executeCommand("workbench.action.moveEditorToBelowGroup")
		}

	revealOrCreate()
	}

def createPatientsPanel(context: ExtensionContext, column: vscode.ViewColumn): Unit = {
	val path = js.Dynamic.global.require("path")
	val panel = vscode.window.createWebviewPanel(
		"Patients", // Internal identifier of the webview panel
		"Patient List", // Title of the panel displayed to the user
		column, // Editor column to show the new webview panel in
		js.Dynamic
			.literal( // Webview options
				enableScripts = true, // Allow JavaScript in the webview
				retainContextWhenHidden = true, // keeping webview alive
				localResourceRoots = js.Array(
					vscode.Uri.file(path.join(context.extensionPath, "media").toString),
					vscode.Uri.file(path.join(context.extensionPath, "out").toString)
				)
			)
			.asInstanceOf[vscode.WebviewPanelOptions & vscode.WebviewOptions]
	)
	// Set the HTML content for the panel
	panel.webview.html = getPatientsListHtml(panel.webview, context)

	// Handle messages from the webview
	panel.webview.onDidReceiveMessage { (message: Any) =>
		handleWebviewMessage(message.asInstanceOf[js.Dynamic])
	}

	// Handle disposal
	panel.onDidDispose((_: Unit) => { // Changed the lambda to accept a Unit argument
		println("Patient panel disposed.")
		patientsPanel = None // Reset the panel reference
	})

	// Store the panel reference and handle disposal
	patientsPanel = Some(panel)
}

def sendMessageToPatientTracker(narrativeTypes: List[Int]): Unit = {
	// Get current active editor's file name and send it to the patient tracker
	val flag = (narrativeTypes.contains(1), narrativeTypes.contains(2)) match {
		case (true, true)   => "12"
		case (true, false)  => "1"
		case (false, true)  => "2"
		case (false, false) => "0"
	}
	val editor = vscode.window.activeTextEditor
	editor.foreach { ed =>
		val document = ed.document
		val path = js.Dynamic.global.require("path")
		val fileName = path.basename(document.fileName).asInstanceOf[String] // Extract the file name
		val unitNumber = fileName.split("\\.").head
		patientsPanel match {
			case Some(p) =>
			p.reveal(null, preserveFocus = true)
			val req = Request(
				MessagingCommands.UpdateNarratives,
				UpdateNarratives(
				source = "vscode-extension",
				unitNumber = unitNumber,
				flag = flag
				)
			)
			p.webview.postMessage(req.data.toJsObject(req.command))
			vscode.window.showInformationMessage(s"Message sent to Patient Tracker: $unitNumber")
			// Ask the webview to refresh just this patient row
			p.webview.postMessage(
				js.Dynamic.literal(
				"command" -> "ReloadPatient",
				"source" -> "vscode-extension",
				"unitNumber" -> unitNumber
				)
			)

			case None =>
			vscode.window.showWarningMessage("Patient Panel not found, message will not be sent.")
		}
	}
}
