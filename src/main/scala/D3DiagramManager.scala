package com.axiom.visual

import typings.vscode.mod as vscode
import typings.std.PromiseLike // Import the exact ST type
import scala.scalajs.js
import com.axiom.protocol.{DiagramNode, Protocol, SourceRange, ToExtension, ToWebview}
import org.aurora.sjsast.scoring.af.Cha2ds2VascRiskFactor
import org.aurora.visual.d3.D3Renderer
import org.aurora.sjsast.LHMap
import concurrent.ExecutionContext.Implicits.global

class D3DiagramManager(extContext: vscode.ExtensionContext) { // Renamed to extContext to avoid shadowing
	private var view: Option[vscode.WebviewView] = None
	// The document the diagram was last drawn from; node clicks and cursor moves are synced against it
	private var currentUri: Option[vscode.Uri] = None
	// The latest diagram, so a webview that (re)loads after it was drawn can be brought up to date
	private var lastDiagram: Option[ToWebview.UpdateDiagram] = None

	val provider: vscode.WebviewViewProvider = new vscode.WebviewViewProvider {
		
		// Ensure exact argument names and return type matching ScalablyTyped
		override def resolveWebviewView(
		webviewView: vscode.WebviewView,
		context: vscode.WebviewViewResolveContext[Any],
		token: vscode.CancellationToken
		): Unit | PromiseLike[Unit] = { 
		
		view = Some(webviewView)

		webviewView.webview.options = vscode.WebviewOptions()
			.setEnableScripts(true)
			.setLocalResourceRoots(js.Array(extContext.extensionUri)) // Use extContext

		webviewView.webview.html = getHtmlForWebview(webviewView.webview)

		webviewView.webview.onDidReceiveMessage { (message: Any) =>
			DiagramChannel.decode(message) match {
				case Right(msg) => handleWebviewMessage(msg)
				case Left(error) => println(s"Ignoring unrecognised message from the D3 webview: $error")
			}
		}
		()
		}
	}

	private def handleWebviewMessage(message: ToExtension): Unit = message match {
		case ToExtension.Ready(version) =>
			if (version != Protocol.Version)
				vscode.window.showErrorMessage(
					s"Aurora diagram is out of date (webview protocol v$version, extension v${Protocol.Version}). Rebuild the D3 bundle."
				)
			else lastDiagram.foreach(send)

		case ToExtension.NodeClicked(_, SourceRange(startLine, startChar, endLine, endChar)) =>
			selectInEditor(startLine, startChar, endLine, endChar)
	}

	private def send(msg: ToWebview): Unit =
		view.foreach(v => DiagramChannel.post(v.webview, msg))

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
			send(ToWebview.HighlightPosition(line, character))
		}
	}

	def updateDiagram(tree: DiagramNode, uri: vscode.Uri): Unit = {
		currentUri = Some(uri)
		val msg = ToWebview.UpdateDiagram(tree)
		lastDiagram = Some(msg)
		send(msg)
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
		|<body>
		|    <div id="d3-container"></div>
 		|    <script type="module" src="$scriptUri"></script>
		|</body>
		|</html>""".stripMargin
	}
}