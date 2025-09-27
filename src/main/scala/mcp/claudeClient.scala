package com.axiom.mcp

import scala.scalajs.js
import scala.scalajs.js.annotation._
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import org.scalajs.dom.experimental.{Fetch, HttpMethod, Headers, RequestInit, Response}

@JSExportTopLevel("ClaudeClient")
object ClaudeClient {

	private val apiKey = js.Dynamic.global.process.env.selectDynamic("CLAUDE_API_KEY").asInstanceOf[js.UndefOr[String]]
	private val model = js.Dynamic.global.process.env.selectDynamic("CLAUDE_MODEL").asInstanceOf[js.UndefOr[String]]

	private val apiUrl = "https://api.anthropic.com/v1/messages"

	def getMcpFromPrompt(prompt: String): Future[js.Dynamic] = {
		val reqHeaders = new Headers()
		reqHeaders.set("x-api-key", apiKey.getOrElse(""))
		reqHeaders.set("anthropic-version", "2023-06-01")
		reqHeaders.set("Content-type", "application/json")

		val reqBody =
			js.JSON.stringify(
				js.Dynamic.literal(
					"model" -> model,
					"max_tokens" -> 1500,
					"temperature" -> 0,
					"system" -> claudeDirections,
					"messages" -> js.Array(
						js.Dynamic.literal(
							"role" -> "user",
							"content" -> prompt
						)
					)
				)
			)

		val req = new RequestInit {
			method = HttpMethod.POST
			this.headers = reqHeaders
			this.body = reqBody
		}

		Fetch.fetch(apiUrl, req).toFuture
			.flatMap(_.json().toFuture)
			.map { jsVal =>
				val dyn = jsVal.asInstanceOf[js.Dynamic]
				if (!js.isUndefined(dyn.selectDynamic("error"))) {
					val msg = dyn.selectDynamic("error").selectDynamic("message").asInstanceOf[String]
					throw new Exception(s"Claude API error: $msg")
				}

				val content = dyn.selectDynamic("content").asInstanceOf[js.UndefOr[js.Array[js.Dynamic]]]
				if (content.isEmpty || content.get.length == 0) {
					throw new Exception("Claude response had no content field!")
				}

				val text = content.get(0).selectDynamic("text").asInstanceOf[String]
				js.JSON.parse(text)
			}

	}
	private val claudeDirections = """You are Aurora Assistant, a JSON-only design agent for medical software.

				Your job is to convert a user's request into a valid MCP (Model Context Protocol) command.

				IMPORTANT: Only respond in raw JSON (no markdown, no comments, no explanations).

				You can handle two types of actions:

				1. NARRATIVE INSERTION - For adding medical text/narratives:
				{
					"action": "insert_narrative",
					"payload": {
						"type": "[one of: normal, urgent, draft, urgent completed, draft completed]",
						"text": "[clear medical instruction or narrative]",
						"line_number": [integer, default to -1 if not provided by user]
					}
				}

				2. VSCODE COMMAND EXECUTION - For running VS Code commands:
				{
					"action": "execute_vscode_command",
					"command": "[VS Code command ID]",
					"args": [optional array of arguments]
				}

				Available VS Code commands:
				- AuroraSjsExt.aurora (Hello Aurora)
				- AuroraSjsExt.patients (Patient Tracker)  
				- AuroraSjsExt.billing (Billing)
				- AuroraSjsExt.processDSL (Merge PCMs)
				- AuroraSjsExt.toggleDiagramLayout (Toggle Diagram Layout)
				- AuroraSjsExt.changeNarrativeType (Change Narrative Type)
				- AuroraSjsExt.hideNarratives (Hide Narratives)
				- AuroraSjsExt.hideNamedGroups (Hide Named Groups)

				Examples:
				- "open patient tracker" → {"action": "execute_vscode_command", "command": "AuroraSjsExt.patients"}
				- "add urgent note: check vitals" → {"action": "insert_narrative", "payload": {"type": "urgent", "text": "check vitals", "line_number": 0}}
				- "toggle diagram layout" → {"action": "execute_vscode_command", "command": "AuroraSjsExt.toggleDiagramLayout"}

				Analyze the user's intent and choose the appropriate action type."""
}
