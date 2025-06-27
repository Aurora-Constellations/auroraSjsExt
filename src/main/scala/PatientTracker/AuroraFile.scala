package com.axiom.AuroraFile

import scala.scalajs.js
import scala.scalajs.js.DynamicImplicits.truthValue
import typings.vscode.mod as vscode

def handleCreate(filename: String): Unit = {
  val fs = js.Dynamic.global.require("fs")
  val path = js.Dynamic.global.require("path")
  val workspaceFolders = vscode.workspace.workspaceFolders

  // Use the first workspace folder as the base path
  workspaceFolders.toOption match {
    case Some(folders) if folders.nonEmpty =>
      val basePath = folders(0).uri.fsPath.asInstanceOf[String]
      println(s"Base path (project directory): $basePath") // Debugging line

      val fullPath = path.join(basePath, filename).asInstanceOf[String] // Path to the file inside auroraFiles

      // If the file doesn't exist, create it
      if (!fs.existsSync(fullPath)) {
        val defaultContent =
          s"""Issues:\n\nOrders:\n"""
        fs.writeFileSync(fullPath, defaultContent)
      }

      // Open the file in the editor
      vscode.workspace.openTextDocument(fullPath).`then` { doc =>
        vscode.window.showTextDocument(doc, js.Dynamic.literal(
            "viewColumn" -> vscode.ViewColumn.One,
            "preview" -> false,
            "preserveFocus" -> false
          ).asInstanceOf[vscode.TextDocumentShowOptions]
        )
      }
    case _ =>
      vscode.window.showErrorMessage("No workspace folder open.")
  }
}

def handleOpen(filename: String): Unit = {
  val fs = js.Dynamic.global.require("fs")
  val path = js.Dynamic.global.require("path")
  val workspaceFolders = vscode.workspace.workspaceFolders

  workspaceFolders.toOption match {
    case Some(folders) if folders.nonEmpty =>
      val basePath = folders(0).uri.fsPath.asInstanceOf[String]
      println(s"Base path (project directory): $basePath") // Debugging line
      val fullPath = path.join(basePath, filename).asInstanceOf[String] // Explicit cast to String

      if (fs.existsSync(fullPath)) {
        vscode.workspace.openTextDocument(fullPath).`then` { doc =>
          vscode.window.showTextDocument(doc, js.Dynamic.literal(
              "viewColumn" -> vscode.ViewColumn.One,
              "preview" -> false,
              "preserveFocus" -> false
            ).asInstanceOf[vscode.TextDocumentShowOptions]
          )
        }
      } else {
        vscode.window.showErrorMessage(s"File '$filename' does not exist.")
      }
    case _ =>
      vscode.window.showErrorMessage("No workspace folder open.")
  }
}
