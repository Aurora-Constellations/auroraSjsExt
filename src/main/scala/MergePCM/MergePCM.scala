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
            val alias = m.group(1)
            val moduleName = m.group(2)
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
            moduleImports.flatMap { 
                case (moduleName, alias) =>
                    val modulePath = path.join(activeFileDir, s"$moduleName.aurora").toString
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

    def parseModules(modules: Map[String, (String, String)]): Future[List[PCM]] = {
        // modules: Map[moduleName -> (modulePath, alias)]
        val moduleEntries = modules.values.toList  // List[(modulePath, alias)        
        val pcmFutures = moduleEntries.map { 
            case (modulePath, alias) =>
                parse(modulePath).toFuture.map { parsed =>
                    try {
                        val modulePCM = ModulePCM(parsed)
                        val pcm = modulePCM.toPCM(alias)
                        pcm
                    } catch {
                        case e: Exception =>
                            println(s"Failed to build PCM from AST: ${e.getMessage}")
                            PCM(Map.empty)
                    }
                }.recover {
                    case e: Exception =>
                        println(s"Module parse error: ${e.getMessage}")
                        PCM(Map.empty)
                }
        }
        Future.sequence(pcmFutures)
    }

    def extractSectionBeforeOrders(content: String): String = {
        // Simple string search instead of regex with multiline flag
        val lines = content.split("\n")
        val ordersIndex = lines.indexWhere(line => line.trim.startsWith("Orders:"))
        
        if (ordersIndex >= 0) {
            println(s"Found Orders: at line $ordersIndex")
            lines.take(ordersIndex).mkString("\n").trim
        } else {
            println("No Orders: section found, keeping entire content")
            content.trim
        }
    }

    def generateOrdersDSL(modules: Map[String, (String, String)]): Future[String] = {
        parseModules(modules).map { modulePCMs =>        
            if (modulePCMs.isEmpty || modulePCMs.forall(_.cio.isEmpty)) {
                println("No valid PCMs to merge")
                ""
            } else {
                val validPCMs = modulePCMs.filter(_.cio.nonEmpty)
                println(s"Valid PCMs: ${validPCMs.size}")
                
                if (validPCMs.isEmpty) {
                    ""
                } else {
                    val mergedPCM = validPCMs.reduce(_ |+| _)
                    println(s"Merged PCM keys: ${mergedPCM.cio.keys}")
                    
                    mergedPCM.cio.get("Orders") match {
                        case Some(orders) =>
                            import ShowAurora.given
                            val ordersStr = orders.asInstanceOf[Orders].show
                            println(s"Generated Orders DSL length: ${ordersStr.length}")
                            ordersStr
                        case None =>
                            println("No Orders in merged PCM")
                            ""
                    }
                }
            }
        }
    }

    def replaceFileContent(newContent: String): Unit = {
        println(s"Replacing file content, new length: ${newContent.length}")
        vscode.window.activeTextEditor.foreach { ed =>
            val document = ed.document
            val lastLine = document.lineCount - 1
            val lastChar = document.lineAt(lastLine).range.end
            val fullRange = new vscode.Range(
                new vscode.Position(0, 0),
                lastChar
            )
            ed.edit { editBuilder =>
                editBuilder.replace(fullRange, newContent)
            }
        }
    }

    def prettyPrint(pcm:PCM):String = 
        import ShowAurora.{given}
        pcm.show
        
        
        
