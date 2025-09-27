package com.axiom.mcp

import scala.scalajs.js
import typings.vscode.mod.{window, workspace, WorkspaceEdit, Position, TextEditorEdit, Uri, commands}
import scala.concurrent.Future
import scala.util.{Success, Failure}

object McpActions:
	
	// Valid VS Code commands for this extension
	private val validCommands = Set(
		"AuroraSjsExt.aurora",
		"AuroraSjsExt.patients", 
		"AuroraSjsExt.billing",
		"AuroraSjsExt.processDSL",
		"AuroraSjsExt.toggleDiagramLayout",
		"AuroraSjsExt.changeNarrativeType",
		"AuroraSjsExt.hideNarratives",
		"AuroraSjsExt.hideNamedGroups"
	)

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

		val doc = editor.document
		val edit = new WorkspaceEdit()

		// Decide insertion line
		val targetLine: Int =
			if (lineNumber == -1) editor.selection.active.line.toInt //The zero-based line value.
			else math.max(0, lineNumber - 1) // shift to 0-based, protect against negatives

		// Get end of target line
		val lineText = doc.lineAt(targetLine).text
		val endPos = new Position(targetLine, lineText.length)

		// Append with a space before if needed
		val insertText = (if (lineText.trim.nonEmpty) " " else "") + finalLine

		edit.insert(doc.uri, endPos, insertText)

		workspace
			.applyEdit(edit)
			.`then`(
				(_: Boolean) => window.showInformationMessage(s"Inserted: $finalLine"),
				(err: Any) => window.showErrorMessage(s"Failed to insert narrative: $err")
			)
	}

	def executeVsCodeCommand(command: String, args: js.Array[js.Any] = js.Array()): Unit = {
		// Validate command
		if (!validCommands.contains(command)) {
			window.showErrorMessage(s"Invalid or unauthorized command: $command")
			println(s"❌ Attempted to execute invalid command: $command")
			return
		}

		// Execute the command
		val execution = if (args.isEmpty) {
			commands.executeCommand(command)
		} else {
			commands.executeCommand(command, args.toArray*)
		}
	}