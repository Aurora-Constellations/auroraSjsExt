import typings.vscode.mod as vscode
import typings.vscode.anon.Dispose
import scala.util.*
import scala.scalajs.js
import concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.annotation._
import vscode.{ExtensionContext}
import PublishCommands.{publishCommands, initRecordingStatusBar}
import typings.auroraLangium.distTypesSrcExtensionLangclientconfigMod.LanguageClientConfigSingleton
// import typings.sprottyVscode.libLspLspSprottyViewProviderMod.LspSprottyViewProvider
import typings.vscode.mod.TextDocument
import PublishCommands.{refreshDiagram}
import com.axiom.patienttracker.sendMessageToPatientTracker
import com.axiom.Narratives.ManageNarratives.getParseNarratives
import com.axiom.visual.D3DiagramManager
import scala.compiletime.uninitialized
import org.aurora.visual.d3.D3Renderer
import org.aurora.sjsast.LHMap
import org.scalajs.dom
import org.scalajs.dom.Element

object AuroraSjsExt {
  val langConfig = LanguageClientConfigSingleton.getInstance()
  var d3Manager: D3DiagramManager = uninitialized



    

  @JSExportTopLevel("activate")
  def activate(context: vscode.ExtensionContext): Unit = {

    // Extension to open a specific folder, i.e. "auroraFiles"
    val path = js.Dynamic.global.require("path")
    val defaultPath = path.join(context.extensionPath, "auroraFiles").toString
    // Create URI and ask to open it as workspace
    val folderUri = vscode.Uri.file(defaultPath)

    // addUpdateDiagramCommand(context)
    /* Note:
      extensions cannot directly change or open a workspace folder programmatically on activation 
      due to VS Code's security and UX model. But here is an acceptable approach
    */
    vscode.commands.executeCommand("vscode.openFolder", folderUri, false) 

    d3Manager = new D3DiagramManager(context)
    context.subscriptions.push(
      vscode.window.registerWebviewViewProvider("aurora", d3Manager.provider).asInstanceOf[Dispose]
    )

     
    vscode.workspace.onDidSaveTextDocument(
  (doc: TextDocument) => {

    println("DOCUMENT SAVED")
    println("Saved file: " + doc.fileName)

    refreshDiagram(doc, d3Manager)

  },
  js.undefined,
  js.undefined
)
    val serverPath = context.asAbsolutePath("node_modules/aurora-langium/dist/cjs/language/main.cjs")
    
    langConfig.asInstanceOf[js.Dynamic].initialize(context, serverPath)
    // langConfig.registerWebviewViewProvider()
    val outputChannel = vscode.window.createOutputChannel("My Extension")  
    outputChannel.appendLine("Congratulations Team Aurora, your extension 'vscode-scalajs-aurora' is now active!")
    outputChannel.show(preserveFocus = true)
    publishCommands(context, langConfig, d3Manager)
    initRecordingStatusBar(context)
  }

  def deactivate(): Unit = {
    langConfig.stopClient()
  }
}
