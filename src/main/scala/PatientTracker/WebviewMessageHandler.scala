package com.axiom

import typings.vscode.mod as vscode
import scala.scalajs.js
import com.axiom.messaging.*
import com.axiom.AuroraFile.{handleCreate, handleOpen}

object WebviewMessageHandler {
    def handleWebviewMessage(message: js.Dynamic): Unit = {
        val command = message.command.asInstanceOf[String]
        command match {
            case MessagingCommands.CreateAuroraFile =>
                println(s"[WebviewMessageHandler] Handling command: $command")
                val typedMsg = message.asInstanceOf[CreateAuroraFileMsg]
                val fileName = typedMsg.fileName
                vscode.window.showInformationMessage(s"Creating file: $fileName")
                handleCreate(fileName)

            case MessagingCommands.OpenAuroraFile =>
                println(s"[WebviewMessageHandler] Handling command: $command")
                val typedMsg = message.asInstanceOf[OpenAuroraFileMsg]
                val fileName = typedMsg.fileName
                vscode.window.showInformationMessage(s"Opening file: $fileName")
                handleOpen(fileName)

            case MessagingCommands.AddedToDB =>
                println(s"[WebviewMessageHandler] Handling command: $command")
                val typedMsg = message.asInstanceOf[AddedToDBMsg]
                val fileName = typedMsg.fileName
                vscode.window.showInformationMessage(s"Added to Database: $fileName")

            case MessagingCommands.UpdatedNarratives =>
                println(s"[WebviewMessageHandler] Handling command: $command")
                val typedMsg = message.asInstanceOf[UpdatedNarrativesMsg]
                val text = typedMsg.message
                vscode.window.showInformationMessage(text)

            case other =>
                vscode.window.showWarningMessage(s"Unknown command: $other")
        }
    }
}