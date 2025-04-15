package com.axiom.patientTracker

import typings.vscode.mod as vscode
import scala.util.Random

object PatientsListHtml:
    // Function to generate dynamic HTML content
    def getPatientsListHtml(webview: vscode.Webview, context: vscode.ExtensionContext): String = {
        val nonce = getNonce()
        // Create an HTML unordered list from the content
        // val listItems = patient.map(item => s"<li>${item.unitNumber}</li>").mkString("\n")
        val scriptUri = webview.asWebviewUri(vscode.Uri.joinPath(context.extensionUri, "media", "PatientTrackerMain.js")).toString;
        // println(s"Script URI: $scriptUri");
        s"""
            |<!DOCTYPE html>
            |<html lang="en">
            |<head>
            |    <meta charset="UTF-8">
            |    <meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src ${webview.cspSource}; img-src ${webview.cspSource} https:; script-src 'nonce-${nonce}'; connect-src 'self' http://localhost:8080;">
            |    <meta name="viewport" content="width=device-width, initial-scale=1.0">
            |    <title>Patient List</title>
            |</head>
            |<body>
            |    <script type="module" src="${scriptUri}" nonce="${nonce}"></script>
            |    <h1>Patient List</h1>
            |    <div id="app"></div>
            |</body>
            |</html>
        """.stripMargin
    }

    def getNonce(): String = {
        val possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        (1 to 32).map(_ => possible(Random.nextInt(possible.length))).mkString
    }