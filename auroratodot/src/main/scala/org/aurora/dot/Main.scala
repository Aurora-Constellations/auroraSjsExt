package org.aurora.dot

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import scala.concurrent.Future
import scala.scalajs.js
import org.scalajs.dom
import org.aurora.sjsast.*
import typings.auroraLangium.distTypesSrcLanguageAuroraModuleMod.createAuroraServices
import typings.langium.mod.EmptyFileSystem
import typings.vscodeUri.mod.URI

object Main:
    def parseString(content: String): scala.concurrent.Future[GenAst.PCM] = { 
        val context = js.Dynamic.literal(
        fileSystemProvider = { (services: js.Any) => EmptyFileSystem }
        ).asInstanceOf[typings.langium.libLspDefaultLspModuleMod.DefaultSharedModuleContext]
        
        val servicesContext = createAuroraServices(context)
        val shared = servicesContext.shared
        
        val uri = URI.parse("file:///model.aurora").asInstanceOf[typings.langium.libUtilsUriUtilsMod.URI]
        val document = shared.workspace.LangiumDocumentFactory.fromString(content, uri)
        
        shared.workspace.DocumentBuilder.build(js.Array(document)).toFuture.map { _ =>
        document.parseResult.value.asInstanceOf[GenAst.PCM]
        }
    }   

    def main(args: Array[String]): Unit = 
        println("--- Starting Aurora to DOT Conversion ---")
        val filePath = "auroratodot/public/TBSimple.aurora"
        println(s"Fetching file from $filePath...")

        val pipeline = for {
            textContent <- Future {
                val fs = js.Dynamic.global.require("fs")
                fs.readFileSync(filePath, "utf-8").asInstanceOf[String]
            }
            
            _ <- Future { println("File loaded successfully! Parsing string...") }
            parsedAst <- parseString(textContent)
            irPCM <- Future { PCM(parsedAst) }
        } yield {
            println("Parsing successful! Converting to IR...")
            val dotOutput = DotGenerator.generate(irPCM)
            println("=== GENERATED DOT ===")
            println(dotOutput)
        }

        pipeline.onComplete {
            case Success(_) => println("Pipeline completed successfully.")
            case Failure(exception) => 
                println(s"Pipeline error: ${exception.getMessage}")
                exception.printStackTrace()
        }