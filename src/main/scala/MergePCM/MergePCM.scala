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

    def prettyPrint(pcm: PCM): String = {
        val sb = new StringBuilder

        // Print issues
        pcm.cio.get("Issues") match {
            case Some(issues: Issues) =>
            sb.append("Issues: ")
            if issues.narrative.nonEmpty then
                val narratives = issues.narrative.map(_.name).toList.sorted
                sb.append(narratives.mkString(" "))
                sb.append("\n")
            issues.ics.toList.sortBy(_.name).foreach { ic =>
            val narrativeStr = 
                if (ic.narrative.nonEmpty)
                " " + ic.narrative.map(_.name).mkString("; ")
                else ""
            sb.append(s"${ic.name}$narrativeStr\n")
            }
            sb.append("\n")
            case _ =>
        }

        // Print orders
        pcm.cio.get("Orders") match {
            case Some(orders: Orders) =>
            sb.append("Orders:\n")
            orders.ngo.foreach { ngo =>
                sb.append(s"${ngo.name}\n")
                ngo.orderCoordinates.foreach { oc =>
                val refsStr = oc.refs.map(_.name).mkString(",")
                if (refsStr.nonEmpty)
                    sb.append(s"${oc.name}($refsStr) \n")
                else
                    sb.append(s"${oc.name} \n")
                }
                sb.append("\n")
            }
            case _ =>
        }

        sb.toString()
    }