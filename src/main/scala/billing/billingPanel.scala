package com.axiom.billing

import typings.vscode.mod as vscode
import vscode.{ExtensionContext}
import scala.scalajs.js
import com.axiom.billing.BillingHtml.getBillingHtml
import com.axiom.WebviewMessageHandler.handleWebviewMessage



private var billingPanel: Option[vscode.WebviewPanel] = None // Store reference to the webview panel

def showBilling(context: vscode.ExtensionContext): js.Function1[Any, Any] =
	(_: Any) => {
	def revealOrCreate(): Unit =
		billingPanel match {
		case Some(panel) if !js.isUndefined(panel) =>
			panel.reveal(vscode.ViewColumn.Two)
		case _ =>
			// Open webview beside, then move to bottom group
			createBillingPanel(context, vscode.ViewColumn.Active)

			// Now move it to bottom group
			vscode.commands.executeCommand("workbench.action.moveEditorToBelowGroup")
		}

	revealOrCreate()
}

def createBillingPanel(context: ExtensionContext, column: vscode.ViewColumn): Unit = {
	val path = js.Dynamic.global.require("path")
	val panel = vscode.window.createWebviewPanel(
		"Billing", // Internal identifier of the webview panel
		"Billing Information", // Title of the panel displayed to the user
		column, // Editor column to show the new webview panel in
		js.Dynamic
			.literal( // Webview options
			enableScripts = true, // Allow JavaScript in the webview
			localResourceRoots = js.Array(
				vscode.Uri.file(path.join(context.extensionPath, "media").toString),
				vscode.Uri.file(path.join(context.extensionPath, "out").toString)
			)
			)
			.asInstanceOf[vscode.WebviewPanelOptions & vscode.WebviewOptions]
	)
	// Set the HTML content for the panel
	panel.webview.html = getBillingHtml(panel.webview, context)

	// Handle messages from the webview
	panel.webview.onDidReceiveMessage { (message: Any) =>
		handleWebviewMessage(message.asInstanceOf[js.Dynamic])
	}

	// Handle disposal
	panel.onDidDispose((_: Unit) => { // Changed the lambda to accept a Unit argument
		println("Billing panel disposed.")
		billingPanel = None // Reset the panel reference
	})

	// Store the panel reference and handle disposal
	billingPanel = Some(panel)
}
