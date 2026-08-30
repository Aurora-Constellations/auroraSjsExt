package org.aurora.utils
import org.aurora.visual.d3.D3Renderer
import org.aurora.sjsast.LHMap
import org.scalajs.dom
import org.aurora.BrowserParser
import typings.langium.libMod.EmptyFileSystem
import scala.scalajs.js
import typings.auroraLangium.distTypesSrcLanguageAuroraModuleMod.createAuroraServices
object Diagram {

  implicit val ec: scala.concurrent.ExecutionContext =  scala.concurrent.ExecutionContext.global

  def renderDiagram(
    textContent: String,
    renderer: D3Renderer,
    layoutOptions: LHMap[String, String]
): Unit = {

  dom.document.body.insertAdjacentHTML(
    "beforeend",
    "<h2>ENTERED renderDiagram</h2>"
  )

  val fs = EmptyFileSystem

  dom.document.body.insertAdjacentHTML(
    "beforeend",
    "<h2>EMPTY FILE SYSTEM OK</h2>"
  )

  val context = js.Dynamic.literal(
  fileSystemProvider = { (services: js.Any) => EmptyFileSystem }
).asInstanceOf[
  typings.langium.libLspDefaultLspModuleMod.DefaultSharedModuleContext
]

  dom.document.body.insertAdjacentHTML(
    "beforeend",
    "<h2>CONTEXT OK</h2>"
  )


}
}