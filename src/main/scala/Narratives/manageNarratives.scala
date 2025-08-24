package com.axiom.Narratives

import typings.vscode.mod as vscode
import vscode.{ExtensionContext}
import scala.scalajs.js
import scala.concurrent.Future
import concurrent.ExecutionContext.Implicits.global
import typings.auroraLangium.cliMod.parse
import scala.util.{Success, Failure}
import typings.vscode.mod.TextEditor
import typings.auroraLangium.cliMod.AstUtils
import typings.auroraLangium.distTypesSrcLanguageGeneratedAstMod.NL_STATEMENT

object ManageNarratives:

  def getParseNarratives(context: ExtensionContext): Future[List[Int]] = {
    val editorOpt = vscode.window.activeTextEditor
    val activeEditor: Option[TextEditor] = Option(editorOpt.asInstanceOf[TextEditor])

    activeEditor match {
      case Some(editor) =>
        parse(editor.document.uri.fsPath).toFuture.map { pcm =>
          val narratives: js.Array[NL_STATEMENT] = AstUtils.streamAllContents(pcm).toArray()
            .filter(_.`$type` == "NL_STATEMENT")
            .map(_.asInstanceOf[NL_STATEMENT])

          val categories: List[Int] = narratives.map { n =>
            n.name.trim.headOption match {
              case Some('!') => 1
              case Some('?') => 2
              case _         => 0
            }
          }.toList
          categories
        }.recover {
          case e =>
            println(s"Failed to parse PCM: ${e}")
            List.empty[Int]
        }

      case None =>
        println("No active editor found.")
        Future.successful(List.empty[Int])
    }
  }


  def changeNarrativesType(context: ExtensionContext): js.Function1[Any, Any] = { _ =>
    val narrativeTypes = Map(
      "normal" -> "--",
      "urgent" -> "!!",
      "draft" -> "??",
      "urgent completed" -> "xx",
      "draft completed" -> ".."
    )

    val editorOpt = vscode.window.activeTextEditor

    editorOpt.foreach { editor =>
      val selection = editor.selection

      // If no region is selected, fallback to current line
      val targetRange =
        if (selection.isEmpty) editor.document.lineAt(selection.active.line).range
        else selection

      val selectedText = editor.document.getText(targetRange)
      val selectedLines = selectedText
        .split("\r?\n")
        .toList // Keep all lines, including empty ones and whitespace

      vscode.window
        .showQuickPick(
          js.Array("normal", "urgent", "draft", "urgent completed", "draft completed"),
          new vscode.QuickPickOptions {
            placeHolder = "Select a narrative type"
          }
        )
        .toFuture
        .flatMap { chosen =>
          if (chosen == null || chosen.toString.trim.isEmpty) Future.successful(())
          else {
            val chosenType = chosen.asInstanceOf[String]
            val symbol = narrativeTypes.getOrElse(chosenType.toLowerCase(), "")
            val updatedLines = selectedLines.map(line => updateNarrativeType(line, symbol))
            editor.edit { editBuilder =>
              val replacement = updatedLines.mkString("\n")
              editBuilder.replace(targetRange, replacement)
            }.toFuture
          }
        }
        .recover { case ex =>
          vscode.window.showErrorMessage(s"Failed to update narratives: ${ex.getMessage}")
        }
    }
  }

  def updateNarrativeType(line: String, newPrefix: String): String = {
    val pattern = raw"""(--|\?\?|!!|xx|\.\.)""".r
    pattern.replaceAllIn(line, newPrefix)
  }
