package com.axiom.mcp

import scala.scalajs.js
import typings.vscode.mod.{window, workspace, WorkspaceEdit, Position, TextEditorEdit, Uri}

object McpActions:
	def insertNarrative(text: String, typ: String, lineNumber: Int): Unit = {
		val editor = window.activeTextEditor.orNull
		if (editor == null) {
			window.showErrorMessage("No active editor found.")
			return
		}

		val symbolMap: js.Dictionary[String] = js.Dictionary(
			"normal" -> "--",
			"urgent" -> "!!",
			"draft" -> "??",
			"urgent completed" -> "xx",
			"draft completed" -> ".."
		)

		val prefix = symbolMap.getOrElse(typ, "--")
		val sanitizedText = text.trim.replaceAll(";*$", "") + ";" // ensure 1 semicolon
		val finalLine = s"$prefix $sanitizedText"

		val edit = new WorkspaceEdit()
		val position = new Position(lineNumber, 0)
		val uri = editor.document.uri

		edit.insert(uri, position, finalLine + "\n")

		workspace
			.applyEdit(edit)
			.`then`(
			(_: Boolean) => window.showInformationMessage(s"Inserted: $finalLine"),
			(err: Any) => window.showErrorMessage(s"Failed to insert narrative: $err")
			)
	}
