package org.aurora.dot

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import scala.scalajs.js
import org.aurora.sjsast.*
import typings.auroraLangium.distTypesSrcLanguageAuroraModuleMod.createAuroraServices
import typings.langium.mod.EmptyFileSystem
import typings.vscodeUri.mod.URI

object Main:
    // Paste your parser function here, or import it if it lives in pcmalgebra
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
        
        // A sample string of your Aurora DSL to test manually
        val sampleAuroraCode = """
        Clinical:
        NGC1:
        CC1

        Issues:
        IC1
        !IC2

        Orders:
        NGO1:
        OC1(?IC1) ?? Draft narrative 1; ?? Draft narrative 2;
        OC2(IC1, !IC2) !! Urgent narrative; -- normal narrative;

        NGO2:
        OC3(~IC2)
        OC4
        """

        // 1. Parse string to GenAst.PCM (Future)
        parseString(sampleAuroraCode).onComplete {
        case Success(genAstPcm) =>
            println("Parsing successful! Converting to IR...")
            
            // 2. Convert Langium AST to your Scala IR
            val irPcm = PCM(genAstPcm)
            
            // 3. Generate DOT string from IR
            val dotOutput = DotGenerator.generate(irPcm)
            
            println("=== GENERATED DOT ===")
            println(dotOutput)
            
        case Failure(exception) =>
            println(s"Parsing failed: ${exception.getMessage}")
        }