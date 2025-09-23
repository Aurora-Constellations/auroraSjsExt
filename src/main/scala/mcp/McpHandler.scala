package com.axiom.mcp

import org.scalajs.dom
import scala.scalajs.js
import com.axiom.mcp.McpActions.{insertNarrative, executeVsCodeCommand}

object McpHandler:
	def action(data: String): Unit = {
		try {
			val parsed = js.JSON.parse(data)
			val action = parsed.selectDynamic("action").toString

			action match {
				case "insert_narrative" =>
					handleInsertNarrative(parsed)
				
				case "execute_vscode_command" =>
					handleExecuteVsCodeCommand(parsed)
				
				case _ =>
					println(s"⚠️ Unknown action: $action")
			}
		} catch {
			case e: Throwable =>
				println(s"❌ Failed to parse MCP message: ${e.getMessage}")
		}
	}

	private def handleInsertNarrative(parsed: js.Dynamic): Unit = {
		try {
			val payload = parsed.selectDynamic("payload")
			val text = payload.selectDynamic("text").toString
			val typ = payload.selectDynamic("type").toString
			val lineNumber = payload.selectDynamic("line_number").asInstanceOf[Int]

			insertNarrative(text, typ, lineNumber)
			println(s"✅ Executed narrative insertion: $text")
		} catch {
			case e: Throwable =>
				println(s"❌ Failed to handle insert_narrative: ${e.getMessage}")
		}
	}

	private def handleExecuteVsCodeCommand(parsed: js.Dynamic): Unit = {
		try {
			val command = parsed.selectDynamic("command").toString
			val argsOption = scala.util.Try(parsed.selectDynamic("args")).toOption
			val args = argsOption match {
				case Some(argsArray) if js.isUndefined(argsArray) => js.Array[js.Any]()
				case Some(argsArray) => argsArray.asInstanceOf[js.Array[js.Any]]
				case None => js.Array[js.Any]()
			}

			executeVsCodeCommand(command, args)
			println(s"✅ Executed VS Code command")
		} catch {
			case e: Throwable =>
				println(s"❌ Failed to handle execute_vscode_command: ${e.getMessage}")
		}
	}