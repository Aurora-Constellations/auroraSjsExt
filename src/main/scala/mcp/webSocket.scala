package com.axiom.mcp

import org.scalajs.dom
import scala.scalajs.js
import com.axiom.mcp.McpActions.{insertNarrative, executeVsCodeCommand}

object McpWebSocket:

	def connect(): Unit =
		// 🔌 Connect to MCP server
		val socket = new dom.WebSocket("ws://localhost:3001")

		socket.onopen = { (_: dom.Event) =>
			println("✅ Connected to MCP Server (Scala.js)")
		}

		socket.onmessage = { (event: dom.MessageEvent) =>
			val data = event.data.toString
			println(s"📩 Received MCP: $data")

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

		socket.onerror = { (error: dom.Event) =>
			println(s"❌ WebSocket error: $error")
		}

		socket.onclose = { (event: dom.CloseEvent) =>
			println(s"🔌 WebSocket connection closed: ${event.reason}")
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
			println(s"✅ Executed VS Code command: $command")
		} catch {
			case e: Throwable =>
				println(s"❌ Failed to handle execute_vscode_command: ${e.getMessage}")
		}
	}