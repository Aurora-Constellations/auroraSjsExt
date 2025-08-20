package com.axiom.mcp

import org.scalajs.dom
import scala.scalajs.js
import com.axiom.mcp.McpActions.insertNarrative

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
				if (action == "insert_narrative") {
					val payload = parsed.selectDynamic("payload")
					val text = payload.selectDynamic("text").toString
					val typ = payload.selectDynamic("type").toString
					val lineNumber = payload.selectDynamic("line_number").asInstanceOf[Int]

					insertNarrative(text, typ, lineNumber)
				}
				else {
					println(s"⚠️ Unknown action: $action")
				}
			} catch {
				case e: Throwable =>
				println(s"❌ Failed to parse MCP message: ${e.getMessage}")
			}
		}
