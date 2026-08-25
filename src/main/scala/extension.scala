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
import org.aurora.utils.Diagram
import org.scalajs.dom
import org.scalajs.dom.Element

object AuroraSjsExt {
  val langConfig = LanguageClientConfigSingleton.getInstance()
  var d3Manager: D3DiagramManager = uninitialized

  def addUpdateDiagramCommand(context: vscode.ExtensionContext) = 
    val command = vscode.commands.registerCommand("updateDiagram", (content: Any) => { /* type signature requires that the input have type "Any" */
      content match {
        case pcmText: String => d3Manager.updateDiagram(pcmText)
        case _ => vscode.window.showInformationMessage("You need to pass the PCM in as a string to update the diagram.")
      }
    })
    context.subscriptions.push(command.asInstanceOf[Dispose])
    

  @JSExportTopLevel("activate")
  def activate(context: vscode.ExtensionContext): Unit = {

    // Extension to open a specific folder, i.e. "auroraFiles"
    val path = js.Dynamic.global.require("path")
    val defaultPath = path.join(context.extensionPath, "auroraFiles").toString
    // Create URI and ask to open it as workspace
    val folderUri = vscode.Uri.file(defaultPath)

    addUpdateDiagramCommand(context)
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
        refreshDiagram(doc, d3Manager) // Pass our manager instead of langConfig
        
        getParseNarratives(context).onComplete {
          case Success(categories) => sendMessageToPatientTracker(categories)
          case Failure(e) => println(s"Failed with error: ${e.getMessage}")
        }
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
    publishCommands(context, langConfig)
    initRecordingStatusBar(context)
  }

  def deactivate(): Unit = {
    langConfig.stopClient()
  }
}
