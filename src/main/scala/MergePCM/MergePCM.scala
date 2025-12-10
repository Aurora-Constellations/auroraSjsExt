package com.axiom.MergePCM

import typings.vscode.mod as vscode
import scala.scalajs.js
import vscode.{ExtensionContext}
import typings.auroraLangium.cliMod.parse
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable
import org.aurora.sjsast.* 
import cats.syntax.semigroup._ // For |+| syntax
import org.aurora.sjsast.catsgivens.given

object MergePCM:
    // Returns Map[moduleName -> alias]
    def parseIssues(input: String): Map[String, String] = {
        val importPattern = """(\w+)\s+from\s+(\w+)""".r
        importPattern.findAllMatchIn(input).map { m =>
            val alias = m.group(1)      // heart_failure
            val moduleName = m.group(2) // CHF
            moduleName -> alias
        }.toMap
    }

    def loadModules(moduleImports: Map[String, String]): Map[String, (String, String)] = {
        // Returns Map[moduleName -> (modulePath, alias)]
        val path = js.Dynamic.global.require("path")
        val fs = js.Dynamic.global.require("fs")
        vscode.window.activeTextEditor.toOption match {
        case Some(editor) =>
            val activeFilePath = editor.document.fileName
            val activeFileDir = path.dirname(activeFilePath)
            moduleImports.flatMap { case (moduleName, alias) =>
                val modulePath = path.join(activeFileDir, s"$moduleName.aurora").toString
                println(s"Loading module from path: $modulePath")
                if (fs.existsSync(modulePath).asInstanceOf[Boolean]) {
                    Some(moduleName -> (modulePath, alias))
                } else {
                    vscode.window.showWarningMessage(s"Module $moduleName not found.")
                    None
                }
            }
        case None =>
            vscode.window.showErrorMessage("No active editor found.")
            Map.empty
        }
    }

    def generateDSL(modules: Map[String, (String, String)]): Future[String] = {
        // modules: Map[moduleName -> (modulePath, alias)]
        val moduleEntries = modules.values.toList  // List[(modulePath, alias)]
        try {
            val pcmFutures = moduleEntries.map { case (modulePath, alias) =>
                parse(modulePath).toFuture.map { parsed =>
                    try {
                        val modulePCM = ModulePCM(parsed)
                        modulePCM.toPCM(alias)  // Use the alias from "heart_failure from CHF"
                    } catch {
                        case e: Exception =>
                            println(s"Failed to build PCM from AST: ${e.getMessage}")
                            PCM(Map.empty)
                    }
                }
            }
            for {
                pcms <- Future.sequence(pcmFutures)
            } yield {
                val mergedPCM = pcms.reduce(_ |+| _)
                println(s"Merged PCM keys: ${mergedPCM.cio.keys}")
                prettyPrint(mergedPCM)
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
        
        
        
