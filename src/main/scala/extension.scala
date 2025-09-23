import typings.vscode.mod as vscode
import typings.vscode.anon.Dispose
import scala.util.*
import scala.scalajs.js
import concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.annotation._
import vscode.{ExtensionContext}
import PublishCommands.{publishCommands, initRecordingStatusBar}
import typings.auroraLangium.distTypesSrcExtensionLangclientconfigMod.LanguageClientConfigSingleton
import typings.sprottyVscode.libLspLspSprottyViewProviderMod.LspSprottyViewProvider
import typings.vscode.mod.TextDocument
import PublishCommands.{refreshDiagram, sendMessageToPatientTracker}
import com.axiom.Narratives.ManageNarratives.getParseNarratives

object AuroraSjsExt {
  val langConfig = LanguageClientConfigSingleton.getInstance()

  @JSExportTopLevel("activate")
  def activate(context: vscode.ExtensionContext): Unit = {

    // Extension to open a specific folder, i.e. "auroraFiles"
    val path = js.Dynamic.global.require("path")
    val defaultPath = path.join(context.extensionPath, "auroraFiles").toString
    // Create URI and ask to open it as workspace
    val folderUri = vscode.Uri.file(defaultPath)
    /* Note:
      extensions cannot directly change or open a workspace folder programmatically on activation 
      due to VS Code's security and UX model. But here is an acceptable approach
    */
    vscode.commands.executeCommand("vscode.openFolder", folderUri, false) 
     
    vscode.workspace.onDidSaveTextDocument(
      (doc: TextDocument) => {
        refreshDiagram(doc, langConfig)
        getParseNarratives(context).onComplete {
          case Success(categories) =>
            sendMessageToPatientTracker(categories)
          case Failure(e) =>
            println(s"Failed with error: ${e.getMessage}")
        }
        }, 
      js.undefined,
      js.undefined
    )
    
    langConfig.setServerModule(context.asAbsolutePath("node_modules/aurora-langium/dist/cjs/language/main.cjs"))
    println(langConfig.getServerModule())
    langConfig.initialize(context)
    langConfig.registerWebviewViewProvider()
    val outputChannel = vscode.window.createOutputChannel("My Extension")  
    outputChannel.appendLine("Congratulations Team Aurora, your extension 'vscode-scalajs-aurora' is now active!")
    outputChannel.show(preserveFocus = true)
    publishCommands(context, langConfig)
    initRecordingStatusBar(context)
  }

  def deactivate(): Unit = {
    langConfig.stopClient()
  }
}
