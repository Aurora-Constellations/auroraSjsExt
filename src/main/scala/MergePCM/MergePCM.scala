package com.axiom.MergePCM

import typings.vscode.mod as vscode
import scala.scalajs.js
import vscode.{ExtensionContext}
import typings.auroraLangium.cliMod.parse
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.{LinkedHashMap, LinkedHashSet}

import org.aurora.sjsast.*
import org.aurora.sjsast.JoinMeet
import org.aurora.sjsast.JoinMeet.given 
import org.aurora.sjsast.Show
import org.aurora.sjsast.Show.given 
import org.aurora.sjsast.Show._ 

// Import the Rewrite utility
import org.aurora.sjsast.RewriteReferences

object MergePCM:

    extension [T](a: T)(using jm: JoinMeet[T])
        def |+|(b: T): T = jm.join(a, b)

    // ... parseIssues and loadModules remain the same ...
    def parseIssues(input: String): Map[String, String] = {
        val importPattern = """(\w+)\s+from\s+(\w+)""".r
        importPattern.findAllMatchIn(input).map { m => m.group(2) -> m.group(1) }.toMap
    }

    def loadModules(moduleImports: Map[String, String]): Map[String, (String, String)] = {
        val path = js.Dynamic.global.require("path")
        val fs = js.Dynamic.global.require("fs")
        vscode.window.activeTextEditor.toOption match {
        case Some(editor) =>
            val activeFilePath = editor.document.fileName
            val activeFileDir = path.dirname(activeFilePath)
            moduleImports.flatMap { case (moduleName, alias) =>
                val modulePath = path.join(activeFileDir, s"$moduleName.aurora").toString
                if (fs.existsSync(modulePath).asInstanceOf[Boolean]) then Some(moduleName -> (modulePath, alias))
                else { vscode.window.showWarningMessage(s"Module $moduleName not found."); None }
            }
        case None => Map.empty
        }
    }

    def parseModules(modules: Map[String, (String, String)]): Future[List[PCM]] = {
        val moduleEntries = modules.values.toList       
        val pcmFutures = moduleEntries.map { 
            case (modulePath, alias) =>
                parse(modulePath).toFuture.map { parsed =>
                    try {
                        val rawModule = ModulePCM(parsed) 
                        
                        // --- FIX: Inject Alias Here ---
                        val aliasedModule = RewriteReferences.addAliasToModule(rawModule, alias)

                        PCM(name = alias, modules = LinkedHashMap(alias -> aliasedModule))

                    } catch {
                        case e: Exception => println(s"Error: ${e.getMessage}"); PCM()
                    }
                }.recover { case e: Exception => println(s"Parse Error: ${e.getMessage}"); PCM() }
        }
        Future.sequence(pcmFutures)
    }

    def generateOrdersDSL(modules: Map[String, (String, String)]): Future[String] = {
        parseModules(modules).map { pcmList =>        
            if (pcmList.isEmpty || pcmList.forall(_.modules.isEmpty)) {
                ""
            } else {
                val validPCMs = pcmList.filter(_.modules.nonEmpty)
                if (validPCMs.isEmpty) "" 
                else {
                    // 1. Merge all PCMs (JoinMeet now handles merging correctly)
                    val mergedPCM = validPCMs.reduce(_ |+| _)
                    println(s"Merged Keys: ${mergedPCM.modules.keys}")

                    // 2. Extract "Orders" sections
                    val allOrders = mergedPCM.modules.values.flatMap { mod =>
                        mod.cio.get("Orders").map(_.asInstanceOf[Orders])
                    }

                    if (allOrders.isEmpty) "" 
                    else {
                        // 3. Merge Orders (JoinMeet merges groups by name)
                        val combinedOrders = allOrders.reduce(_ |+| _)
                        
                        // 4. Show
                        combinedOrders.show
                    }
                }
            }
        }
    }
    
    def extractSectionBeforeOrders(content: String): String = {
        val lines = content.split("\n")
        val ordersIndex = lines.indexWhere(line => line.trim.startsWith("Orders:"))
        if (ordersIndex >= 0) lines.take(ordersIndex).mkString("\n").trim else content.trim
    }

    def replaceFileContent(newContent: String): Unit = {
        vscode.window.activeTextEditor.foreach { ed =>
            val lastLine = ed.document.lineCount - 1
            val lastChar = ed.document.lineAt(lastLine).range.end
            val fullRange = new vscode.Range(new vscode.Position(0, 0), lastChar)
            ed.edit(_.replace(fullRange, newContent))
        }
    }

    def prettyPrint(pcm: PCM): String = pcm.show