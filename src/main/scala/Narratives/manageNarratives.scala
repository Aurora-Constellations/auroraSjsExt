package com.axiom.Narratives

import typings.vscode.mod as vscode
import vscode.{ExtensionContext}
import scala.scalajs.js
import scala.concurrent.Future
import concurrent.ExecutionContext.Implicits.global

object ManageNarratives:
    def changeNarrativesType(context: ExtensionContext): js.Function1[Any, Any] = { _ =>
        val narrativeTypes = Map(
          "normal"   -> "--",
          "urgent"   -> "!!",
          "draft"    -> "??",
          "urgent completed" -> "xx",
          "draft completed"  -> "..",
        )
    
        val editorOpt = vscode.window.activeTextEditor
      
        editorOpt.foreach { editor =>
          val selection = editor.selection
          val selectedText = editor.document.getText(selection)
          val selectedLines = selectedText
            .split("\r?\n")
            .toList // Keep all lines, including empty ones and whitespace
      
          vscode.window
            .showQuickPick(js.Array("normal", "urgent", "draft", "urgent completed","draft completed"),
              new vscode.QuickPickOptions {
                placeHolder = "Select a narrative type"
              })
            .toFuture
            .flatMap { chosen =>
              if (chosen == null || chosen.toString.trim.isEmpty) Future.successful(())
              else {
                val chosenType = chosen.asInstanceOf[String]
                val symbol = narrativeTypes.getOrElse(chosenType.toLowerCase(), "")
                val updatedLines = selectedLines.map(line => updateNarrativeType(line, symbol))
                editor.edit { editBuilder =>
                  val replacement = updatedLines.mkString("\n")
                  editBuilder.replace(selection, replacement)
                }.toFuture
              }
            }
            .recover {
              case ex => vscode.window.showErrorMessage(s"Failed to update narratives: ${ex.getMessage}")
            }
        }
      }
      
      def updateNarrativeType(line: String, newPrefix: String): String = {
        val pattern = raw"""(--|\?\?|!!|xx|\.\.)""".r
        pattern.replaceFirstIn(line, newPrefix)
      }
      