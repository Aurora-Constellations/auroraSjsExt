package com.axiom.MergePCM

import typings.vscode.mod as vscode
import scala.scalajs.js
import vscode.{ExtensionContext}
import typings.auroraLangium.cliMod.parse
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable
import docere.sjsast.*

object MergePCM:
    def parseIssues(input: String): List[String] = {
        val issuePattern = """\bfrom\s+(\w+)""".r
        issuePattern.findAllMatchIn(input).map(_.group(1)).toList
    }

    def loadModules(moduleNames: List[String]): Map[String, String] = {
        val path = js.Dynamic.global.require("path")
        val fs = js.Dynamic.global.require("fs")
        vscode.window.activeTextEditor.toOption match {
        case Some(editor) =>
            val activeFilePath = editor.document.fileName
            val activeFileDir = path.dirname(activeFilePath)
            moduleNames.flatMap { moduleName =>
                val modulePath = path.join(activeFileDir, s"$moduleName.aurora").toString
                val fileContent = fs.readFileSync(modulePath, "utf8").asInstanceOf[String]
                println(s"Loading module from path: $modulePath")
                if (fs.existsSync(modulePath).asInstanceOf[Boolean]) {
                    Some(moduleName -> modulePath)
                } else {
                    vscode.window.showWarningMessage(s"Module $moduleName not found.")
                    None
                }
            }.toMap
        case None =>
            vscode.window.showErrorMessage("No active editor found.")
            Map.empty[String, String]
        }
    }

    def generateDSL(modules: Map[String, String]): Future[String] = {
        val modulePaths = modules.values.toList

        try {
            val pcmFutures = modulePaths.map { modulePath =>
                parse(modulePath).toFuture.map { parsed =>
                    try {
                        val pcm = PCM(parsed)
                        pcm
                    } catch {
                        case e: Exception =>
                            println(s"Failed to build PCM from AST: ${e.getMessage}")
                            PCM(Map.empty)
                    }
                }
            }
            for{
                pcms <- Future.sequence(pcmFutures)
            } yield {
                // println(s"Parsed PCMs: $pcms")
                val mergedPCM = pcms.reduce(_.merge(_))
                println(s"Merged PCM keys: ${mergedPCM.cio.keys}")
                val mergedResults = prettyPrint(mergedPCM)
                // println(s"Merged results: $mergedResults")
                mergedResults
            }
        } catch {
          case e: Throwable =>
            Future.failed(new Exception("Failed to merge files: " + e.getMessage()))
        }
    }

    def updateCurrentFile(context: ExtensionContext, generatedDSL: String): Unit = {
        println(s"Updating current file with generated DSL...")
        val editor = vscode.window.activeTextEditor
        editor.foreach { ed =>
            val document = ed.document
            val lastLine = document.lineCount - 1
            val position = document.lineAt(lastLine).range.end
            ed.edit(editBuilder => {
            editBuilder.insert(position, s"\n\n$generatedDSL") // Add new line before inserting
            })
        }
    }

    def prettyPrint(pcm:PCM):String = 
        import ShowAurora.{given}
        pcm.show
        
        
        
