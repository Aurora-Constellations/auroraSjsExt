package com.axiom.MergePCM

import typings.vscode.mod as vscode
import scala.scalajs.js
import vscode.{ExtensionContext}
import typings.auroraLangium.cliMod.parse
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}

import org.aurora.sjsast.*
import org.aurora.sjsast.JoinMeet._
import org.aurora.sjsast.JoinMeet.given 
import org.aurora.sjsast.Show.given 
import org.aurora.sjsast.Show._ 
import org.aurora.sjsast.GenAst

// Use alias to distinguish between the two PCM types
import org.aurora.sjsast.{PCM => ProcessedPCM}

object MergePCM:

    def parseIssues(currentPCM: GenAst.PCM): Map[String, String] = {
        // Extract imports from Issues section
        currentPCM.elements.flatMap { element =>
            if (element.$type == "Issues") {
                val issues = element.asInstanceOf[GenAst.Issues]
                issues.coord.flatMap { coord =>
                    val alias = coord.name
                    
                    // Get the module name from the first mod reference using $refText
                    coord.mods.headOption match {
                        case Some(modRef) =>
                            // Use $refText which contains the actual text reference
                            val refTextOpt = modRef.asInstanceOf[js.Dynamic].selectDynamic("$refText")
                            if (refTextOpt != js.undefined) {
                                val moduleName = refTextOpt.asInstanceOf[String]
                                Some(moduleName -> alias)
                            } else {
                                // println(s"No refText for $alias")
                                None
                            }
                        case None =>
                            // println(s"No mods for coordinate $alias")
                            None
                    }
                }.toSeq
            } else Seq.empty[(String, String)]
        }.toMap
    }

    def getModuleURIs(currentPCM: GenAst.PCM, moduleNames: Set[String]): Map[String, String] = {
        currentPCM.$document.toOption match {
            case Some(doc) =>
                val currentURI = doc.uri.toString
                val url = js.Dynamic.global.require("url")
                val path = js.Dynamic.global.require("path")
                
                // Use Node.js URL API to properly convert file:// URI to file path
                // This handles Windows paths correctly across all platforms
                val fileURLToPath = url.asInstanceOf[js.Dynamic].fileURLToPath
                val filePath = fileURLToPath(currentURI).asInstanceOf[String]
                
                val baseDir = path.dirname(filePath).asInstanceOf[String]
                
                moduleNames.map { moduleName =>
                    val modulePath = path.join(baseDir, s"$moduleName.aurora").toString
                    moduleName -> modulePath
                }.toMap
            case None =>
                Map.empty
        }
    }

    def parseModulesFromURIs(moduleURIs: Map[String, String], aliases: Map[String, String]): Future[List[ProcessedPCM]] = {
        
        val pcmFutures = moduleURIs.map { case (moduleName, modulePath) =>
            
            parse(modulePath).toFuture.map { parsed =>
                try {
                    // Convert GenAst.PCM to ProcessedPCM
                    val astPCM = parsed.asInstanceOf[GenAst.PCM]
                    val module = Module(astPCM)
                    val modulePCM = ModulePCM(module)
                    val alias = aliases.getOrElse(moduleName, moduleName)
                    val result = modulePCM.toPCM(alias)
                    result
                } catch {
                    case e: Exception => 
                        println(s"Error converting $moduleName: ${e.getMessage}")
                        e.printStackTrace()
                        ProcessedPCM()
                }
            }.recover { 
                case e: Exception => 
                    println(s"Parse error for $moduleName: ${e.getMessage}")
                    e.printStackTrace()
                    ProcessedPCM() 
            }
        }.toList
        
        Future.sequence(pcmFutures)
    }

    def generateOrdersDSL(currentPCM: GenAst.PCM): Future[String] = {
        val moduleImports = parseIssues(currentPCM)
        val moduleNames = moduleImports.keySet
        val moduleURIs = getModuleURIs(currentPCM, moduleNames)
        
        parseModulesFromURIs(moduleURIs, moduleImports).map { modulePCMs =>
            val validPCMs = modulePCMs.filter(_.cio.nonEmpty)
            
            if (validPCMs.isEmpty) {
                println("No valid ProcessedPCMs to merge")
                ""
            } else {
                val mergedPCM = validPCMs.reduce(_ |+| _)                
                mergedPCM.cio.get("Orders") match {
                    case Some(orders) => 
                        val result = orders.asInstanceOf[Orders].show
                        result
                    case None => 
                        println("No Orders in merged ProcessedPCM")
                        ""
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

    def prettyPrint(pcm: ProcessedPCM): String = pcm.show