package org.aurora

import scala.scalajs.js
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import typings.vscodeUri.mod.URI
import org.aurora.sjsast.*

import typings.auroraLangium.distTypesSrcLanguageAuroraModuleMod.createAuroraServices
import typings.langium.mod.EmptyFileSystem
import scala.scalajs.js.JSConverters.*

object BrowserParser:
  def parseString(content: String): js.Promise[GenAst.PCM] = {
    
    // 1. Create the exact context shape Langium expects: 
    // An object with a fileSystemProvider property that is a function returning EmptyFileSystem.
    val context = js.Dynamic.literal(
      fileSystemProvider = { (services: js.Any) => EmptyFileSystem }
    ).asInstanceOf[typings.langium.libLspDefaultLspModuleMod.DefaultSharedModuleContext]
    
    val servicesContext = createAuroraServices(context)
    val shared = servicesContext.shared
    
    // 2. Cast URI to the internal Langium URI type
    val uri = URI.parse("file:///model.aurora").asInstanceOf[typings.langium.libUtilsUriUtilsMod.URI]
    
    // 3. Create the document
    val document = shared.workspace.LangiumDocumentFactory.fromString(content, uri)
    
    // 4. Build and return the parsed AST
    shared.workspace.DocumentBuilder.build(js.Array(document)).toFuture.map { _ =>
      document.parseResult.value.asInstanceOf[GenAst.PCM]
    }.toJSPromise
  }