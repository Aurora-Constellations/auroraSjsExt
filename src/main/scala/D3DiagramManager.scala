package com.axiom.visual

import typings.vscode.mod as vscode
import typings.std.PromiseLike // Import the exact ST type
import scala.scalajs.js
import org.aurora.sjsast.scoring.af.Cha2ds2VascRiskFactor
import org.aurora.utils.Diagram
import org.aurora.visual.d3.D3Renderer
import org.aurora.sjsast.LHMap
import concurrent.ExecutionContext.Implicits.global

class D3DiagramManager(extContext: vscode.ExtensionContext) { // Renamed to extContext to avoid shadowing
	private var view: Option[vscode.WebviewView] = None

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
		}
	}

	def updateDiagram(content: String): Unit = {
		view.foreach { v =>
		v.webview.postMessage(js.Dynamic.literal(
			command = "updateDiagram",
			data = content
		))
		}
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
		|    <script src="$scriptUri"></script>
		|</body>
		|</html>""".stripMargin
	}
}