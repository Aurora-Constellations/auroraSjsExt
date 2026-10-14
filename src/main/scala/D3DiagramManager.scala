package com.axiom.visual

import typings.vscode.mod as vscode
import typings.std.PromiseLike // Import the exact ST type
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import org.aurora.sjsast.scoring.af.Cha2ds2VascRiskFactor
import org.aurora.visual.d3.D3Renderer
import org.aurora.sjsast.LHMap
import concurrent.ExecutionContext.Implicits.global

// presentationMode: the end-user layout (`sbt openUI`), which renames the view and uses the redesigned diagram
class D3DiagramManager(extContext: vscode.ExtensionContext, presentationMode: Boolean = false) { // Renamed to extContext to avoid shadowing
	private var view: Option[vscode.WebviewView] = None
	// The document the diagram was last drawn from; node clicks and cursor moves are synced against it
	private var currentUri: Option[vscode.Uri] = None
	// Short status shown beside the view title, e.g. while recording
	private var status: Option[String] = None
	// Last diagram sent, replayed when the end-user webview is rebuilt (it is discarded each time the view is hidden)
	private var lastDiagram: Option[js.Dynamic] = None

	val provider: vscode.WebviewViewProvider = new vscode.WebviewViewProvider {
		
		// Ensure exact argument names and return type matching ScalablyTyped
		override def resolveWebviewView(
		webviewView: vscode.WebviewView,
		context: vscode.WebviewViewResolveContext[Any],
		token: vscode.CancellationToken
		): Unit | PromiseLike[Unit] = { 
		
		view = Some(webviewView)
		if (presentationMode) webviewView.title = "Care Scenario"
		webviewView.description = status.orUndefined

		webviewView.webview.options = vscode.WebviewOptions()
			.setEnableScripts(true)
			.setLocalResourceRoots(js.Array(extContext.extensionUri)) // Use extContext

		webviewView.webview.html = getHtmlForWebview(webviewView.webview)

		webviewView.webview.onDidReceiveMessage { (message: Any) =>
			handleWebviewMessage(message.asInstanceOf[js.Dynamic])
		}
		()
		}
	}

	private def handleWebviewMessage(message: js.Dynamic): Unit = {
		// Only the end-user diagram sends "ready"
		if (message.command.asInstanceOf[String] == "ready") {
			for (v <- view; diagram <- lastDiagram) v.webview.postMessage(diagram)
		} else if (message.command.asInstanceOf[String] == "nodeClicked" && !js.isUndefined(message.range)) {
			val range = message.range
			selectInEditor(
				range.startLine.asInstanceOf[Int],
				range.startChar.asInstanceOf[Int],
				range.endLine.asInstanceOf[Int],
				range.endChar.asInstanceOf[Int]
			)
		}
	}

	// Selects and reveals the text behind a clicked node, moving focus to the editor
	private def selectInEditor(startLine: Int, startChar: Int, endLine: Int, endChar: Int): Unit = {
		currentUri.foreach { uri =>
			val options = js.Dynamic.literal(
				"preserveFocus" -> false,
				"preview" -> false,
				"selection" -> new vscode.Selection(startLine, startChar, endLine, endChar)
			)
			// Reuse the column the file is already open in instead of opening it a second time
			vscode.window.visibleTextEditors
				.find(_.document.uri.toString() == uri.toString())
				.flatMap(_.viewColumn.toOption)
				.foreach(column => options.updateDynamic("viewColumn")(column.asInstanceOf[js.Any]))

			vscode.workspace.openTextDocument(uri).`then` { doc =>
				vscode.window.showTextDocument(doc, options.asInstanceOf[vscode.TextDocumentShowOptions])
			}
		}
	}

	// Called when the cursor moves in an editor; the webview highlights the node at that position
	def highlightPosition(uri: vscode.Uri, line: Int, character: Int): Unit = {
		if (currentUri.exists(_.toString() == uri.toString())) {
			view.foreach { v =>
				v.webview.postMessage(
					js.Dynamic.literal(
						command = "highlightPosition",
						line = line,
						character = character
					)
				)
			}
		}
	}

	def setStatus(text: Option[String]): Unit = {
		status = text
		view.foreach(_.description = text.orUndefined)
	}

	def updateDiagram(data: js.Any, uri: vscode.Uri): Unit = {
	currentUri = Some(uri)
	val path = js.Dynamic.global.require("path")
	val message = js.Dynamic.literal(
		command = "updateDiagram",
		data = data,
		// The patient's name as shown in the end-user diagram header: the file name without its extension
		title = path.basename(uri.fsPath, path.extname(uri.fsPath))
	)
	lastDiagram = Some(message)
	view.foreach(_.webview.postMessage(message))
	}

	private def getHtmlForWebview(webview: vscode.Webview): String = {
		val scriptUri = webview.asWebviewUri(
		vscode.Uri.joinPath(extContext.extensionUri, "media", "d3_main.js")
		)
		val cssUri = webview.asWebviewUri(
		vscode.Uri.joinPath(extContext.extensionUri, "media", "d3_styles.css")
		)

		s"""<!DOCTYPE html>
		|<html lang="en">
		|<head>
		|    <meta charset="UTF-8">
		|    <meta name="viewport" content="width=device-width, initial-scale=1.0">
		|    <title>Aurora D3 Visualization</title>
		|    <link href="$cssUri" rel="stylesheet">
		|	 <script>
		|    window.global = window;
		|    
		|    // Expanded process mock
		|    window.process = { 
		|      env: {}, 
		|      argv: [], // Fixes the 'length' error in setupExitTimer
		|      on: function() {}, // Mocks process event listeners
		|      cwd: function() { return '/'; }, // Mocks current working directory
		|      platform: 'browser'
		|    };
		|    
		|    // Buffer mock
		|    window.Buffer = window.Buffer || {
		|      isBuffer: function() { return false; },
		|      from: function() { return new Uint8Array(); },
		|      alloc: function(size) { return new Uint8Array(size || 0); },
		|      allocUnsafe: function(size) { return new Uint8Array(size || 0); },
		|      byteLength: function() { return 0; },
		|      concat: function() { return new Uint8Array(); }
		|    };
		|  </script>
		|</head>
		|<body${if (presentationMode) " data-aurora-mode=\"app\"" else ""}>
		|    <div id="d3-container"></div>
 		|    <script type="module" src="$scriptUri"></script>
		|</body>
		|</html>""".stripMargin
	}
}