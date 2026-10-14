import typings.vscode.mod as vscode
import typings.vscode.anon.Dispose
import scala.util.*
import scala.scalajs.js
import concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.annotation._
import vscode.{ExtensionContext}
import PublishCommands.{publishCommands, initRecordingStatusBar}
import typings.auroraLangium.distTypesSrcExtensionLangclientconfigMod.LanguageClientConfigSingleton
import typings.vscode.mod.TextDocument
import PublishCommands.{refreshDiagram}
import com.axiom.patienttracker.sendMessageToPatientTracker
import com.axiom.Narratives.ManageNarratives.getParseNarratives
import com.axiom.visual.D3DiagramManager
import com.axiom.patients.PatientsView
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
    /* Note:
      extensions cannot directly change or open a workspace folder programmatically on activation 
      due to VS Code's security and UX model. But here is an acceptable approach
    */
    // `sbt openUI` already passes the folder on the command line; re-opening it would only reload the window
    val folderAlreadyOpen = vscode.workspace.workspaceFolders.toOption
      .flatMap(_.headOption)
      .exists(f => path.relative(f.uri.fsPath, defaultPath).asInstanceOf[String] == "")
    if (!folderAlreadyOpen) vscode.commands.executeCommand("vscode.openFolder", folderUri, false)

    // End-user layout (set in the profile `sbt openUI` launches): show the Aurora views, keep developer panels closed
    val presentationMode =
      vscode.workspace.getConfiguration("aurora").get[Boolean]("presentationMode").getOrElse(false)

    d3Manager = new D3DiagramManager(context, Option.when(presentationMode)("Care Scenario"))
    context.subscriptions.push(
      vscode.window.registerWebviewViewProvider("aurora", d3Manager.provider).asInstanceOf[Dispose]
    )

    // End-user layout only: a Patients list replaces the file Explorer
    val patientsView = Option.when(presentationMode)(new PatientsView(context))
    patientsView.foreach(_.register())

    if (presentationMode) {
      vscode.commands.executeCommand("workbench.view.extension.aurora-container")
      vscode.commands.executeCommand("workbench.action.closePanel")
    }

    // on start up
    vscode.window.activeTextEditor.toOption match {
      case None => /* do nothig */
      case Some(editor) => refreshDiagram(editor.document, d3Manager) 
    }

    // End-user layout only: switching to another patient redraws the diagram for that patient
    if (presentationMode) {
      context.subscriptions.push(
        vscode.window.onDidChangeActiveTextEditor(
          (editor: js.UndefOr[vscode.TextEditor]) => {
            editor.toOption.foreach { ed =>
              refreshDiagram(ed.document, d3Manager)
              patientsView.foreach(_.select(ed.document))
            }
          },
          js.undefined,
          js.undefined
        ).asInstanceOf[Dispose]
      )
    }

     
    // Cursor in the editor -> highlight the matching node in the diagram
    context.subscriptions.push(
      vscode.window.onDidChangeTextEditorSelection(
        (e: vscode.TextEditorSelectionChangeEvent) => {
          e.selections.headOption.foreach { selection =>
            d3Manager.highlightPosition(
              e.textEditor.document.uri,
              selection.active.line.toInt,
              selection.active.character.toInt
            )
          }
        },
        js.undefined,
        js.undefined
      ).asInstanceOf[Dispose]
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
    // langConfig.asInstanceOf[js.Dynamic].initialize(context, serverPath)
    langConfig.setServerModule(serverPath)
    langConfig.initialize(context)
    // langConfig.registerWebviewViewProvider()
    val outputChannel = vscode.window.createOutputChannel("My Extension")  
    outputChannel.appendLine("Congratulations Team Aurora, your extension 'vscode-scalajs-aurora' is now active!")
    if (!presentationMode) outputChannel.show(preserveFocus = true)
    publishCommands(context, langConfig, d3Manager)
    initRecordingStatusBar(context, Option.when(presentationMode)(d3Manager))
  }

  def deactivate(): Unit = {
    langConfig.stopClient()
  }
}
