import typings.vscode.mod as vscode
import vscode.{ExtensionContext}
import scala.scalajs.js
import typings.vscode.anon.Dispose
import scala.util.*
import concurrent.ExecutionContext.Implicits.global
import com.axiom.patientTracker.PatientsListHtml.getPatientsListHtml
import com.axiom.billing.BillingHtml.getBillingHtml
import docere.sjsast.*
import cats.implicits.toShow
import cats.syntax.all.toShow
import docere.sjsast.toShow
import cats.syntax.show.toShow
import com.axiom.MergePCM.MergePCM.*
import com.axiom.WebviewMessageHandler.handleWebviewMessage
import typings.sprottyVscode.libLspLspSprottyViewProviderMod.LspSprottyViewProvider
import typings.vscode.mod.TextDocument
import typings.auroraLangium.distTypesSrcExtensionLangclientconfigMod.LanguageClientConfigSingleton
import typings.vscode.mod.OutputChannel
import typings.auroraLangium.distTypesSrcExtensionSrcCommandsToggleDiagramLayoutCommandMod.toggleDiagramLayout
import com.axiom.Narratives.ManageNarratives.changeNarrativesType
import typings.auroraLangium.distTypesSrcExtensionSrcCommandsHideNarrativesCommandMod.hideNarratives
import typings.auroraLangium.distTypesSrcExtensionSrcCommandsHideNgosCommandMod.hideNGOs
import typings.vscode.mod.TextEditor
import typings.auroraLangium.cliMod.parse
import com.axiom.messaging.*
import com.axiom.mcp.ClaudeClient
import com.axiom.mcp.McpHandler

object PublishCommands:
  private var patientsPanel: Option[vscode.WebviewPanel] = None // Store reference to the webview panel
  private var billingPanel: Option[vscode.WebviewPanel] = None // Store reference to the webview panel

  def publishCommands(context: ExtensionContext, langConfig: LanguageClientConfigSingleton): Unit = {
      val commands = List(
          ("AuroraSjsExt.aurora", showHello()),
          ("AuroraSjsExt.patients", showPatients(context)),
          ("AuroraSjsExt.billing", showBilling(context)),
          ("AuroraSjsExt.processDSL", processDSL(context)),
          ("AuroraSjsExt.toggleDiagramLayout", toggleLayout(langConfig)),
          ("AuroraSjsExt.changeNarrativeType", changeNarrativesType(context)),
          ("AuroraSjsExt.hideNarratives", hideNarrs(langConfig)),
          ("AuroraSjsExt.hideNamedGroups", hideNamedGroups(langConfig)),
          ("AuroraSjsExt.mcp", takeMcpPrompt(context))
      )

      commands.foreach { case (name, fun) =>
          context.subscriptions.push(
              vscode.commands
                  .registerCommand(name, fun)
                  .asInstanceOf[Dispose]
          )
      }
  }

  def takeMcpPrompt(context: ExtensionContext): js.Function1[Any, Any] = { _ =>
    vscode.window.showInputBox().toFuture.onComplete {
      case Success(prompt) if prompt.toString().trim.nonEmpty =>
        ClaudeClient.getMcpFromPrompt(prompt.toString()).map { mcpJson =>
          val mcpString = js.JSON.stringify(mcpJson)
          McpHandler.action(mcpString)
        }.recover {
          case e: Throwable =>
            val errMsg = s"Error calling Claude API: ${e.getMessage}"
            vscode.window.showErrorMessage(errMsg)
        }
      case _ =>
        val msg = "No prompt provided."
        vscode.window.showWarningMessage(msg)
    }
  }

  def processDSL(context: ExtensionContext): js.Function1[Any, Any] = { _ =>
    val editor = vscode.window.activeTextEditor
    editor.foreach { ed =>
      val document = ed.document
      val text = document.getText()
      val issuesSection = text.split("\n").takeWhile(!_.startsWith("//")).mkString("\n")
      val moduleNames = parseIssues(issuesSection)
      val modules = loadModules(moduleNames)
      val generatedDSL = generateDSL(modules)
      generatedDSL.onComplete {
        case Success(result) => updateCurrentFile(context, result)
        case Failure(e)      => vscode.window.showErrorMessage(s"Error generating DSL: ${e.getMessage}")
      }
    }
  }
  
  def showHello(): js.Function1[Any, Any] = {
      (arg) => {
          vscode.window.showInputBox().toFuture.onComplete {
              case Success(input) => vscode.window.showInformationMessage(s"Hello $input!")
              case Failure(e)     => println(e.getMessage)
          }
      }
  }

  def showPatients(context: vscode.ExtensionContext): js.Function1[Any, Any] =
    (_: Any) => {
      def revealOrCreate(): Unit =
        patientsPanel match {
          case Some(panel) if !js.isUndefined(panel) =>
            panel.reveal(vscode.ViewColumn.Two)
          case _ =>
            // Open webview beside, then move to bottom group
            createPatientsPanel(context, vscode.ViewColumn.Active)

            // Now move it to bottom group
            vscode.commands.executeCommand("workbench.action.moveEditorToBelowGroup")
        }

      revealOrCreate()
    }

  def createPatientsPanel(context: ExtensionContext, column: vscode.ViewColumn): Unit = {
    val path = js.Dynamic.global.require("path")
    val panel = vscode.window.createWebviewPanel(
      "Patients", // Internal identifier of the webview panel
      "Patient List", // Title of the panel displayed to the user
      column, // Editor column to show the new webview panel in
      js.Dynamic
        .literal( // Webview options
          enableScripts = true, // Allow JavaScript in the webview
          retainContextWhenHidden = true, // keeping webview alive
          localResourceRoots = js.Array(
            vscode.Uri.file(path.join(context.extensionPath, "media").toString),
            vscode.Uri.file(path.join(context.extensionPath, "out").toString)
          )
        )
        .asInstanceOf[vscode.WebviewPanelOptions & vscode.WebviewOptions]
    )
    // Set the HTML content for the panel
    panel.webview.html = getPatientsListHtml(panel.webview, context)
    
    // Handle messages from the webview
    panel.webview.onDidReceiveMessage { (message: Any) =>
      handleWebviewMessage(message.asInstanceOf[js.Dynamic])
    }


    // Handle disposal
    panel.onDidDispose((_: Unit) => { // Changed the lambda to accept a Unit argument
      println("Patient panel disposed.")
      patientsPanel = None // Reset the panel reference
    })

    // Store the panel reference and handle disposal
    patientsPanel = Some(panel)
  }

  def showBilling(context: vscode.ExtensionContext): js.Function1[Any, Any] =
    (_: Any) => {
      def revealOrCreate(): Unit =
        billingPanel match {
          case Some(panel) if !js.isUndefined(panel) =>
            panel.reveal(vscode.ViewColumn.Two)
          case _ =>
            // Open webview beside, then move to bottom group
            createBillingPanel(context, vscode.ViewColumn.Active)

            // Now move it to bottom group
            vscode.commands.executeCommand("workbench.action.moveEditorToBelowGroup")
        }

      revealOrCreate()
    }

  def createBillingPanel(context: ExtensionContext, column: vscode.ViewColumn): Unit = {
    val path = js.Dynamic.global.require("path")
    val panel = vscode.window.createWebviewPanel(
      "Billing", // Internal identifier of the webview panel
      "Billing Information", // Title of the panel displayed to the user
      column, // Editor column to show the new webview panel in
      js.Dynamic
        .literal( // Webview options
          enableScripts = true, // Allow JavaScript in the webview
          localResourceRoots = js.Array(
            vscode.Uri.file(path.join(context.extensionPath, "media").toString),
            vscode.Uri.file(path.join(context.extensionPath, "out").toString)
          )
        )
        .asInstanceOf[vscode.WebviewPanelOptions & vscode.WebviewOptions]
    )
    // Set the HTML content for the panel
    panel.webview.html = getBillingHtml(panel.webview, context)

    // Handle messages from the webview
    panel.webview.onDidReceiveMessage { (message: Any) =>
      handleWebviewMessage(message.asInstanceOf[js.Dynamic])
    }

    // Handle disposal
    panel.onDidDispose((_: Unit) => { // Changed the lambda to accept a Unit argument
      println("Billing panel disposed.")
      billingPanel = None // Reset the panel reference
    })

    // Store the panel reference and handle disposal
    billingPanel = Some(panel)
  }

  def toggleLayout(langConfig: LanguageClientConfigSingleton): js.Function1[Any, Any] = {
    (args) => {
      toggleDiagramLayout(langConfig)
    }
  }

  def refreshDiagram(document: TextDocument, langConfig: LanguageClientConfigSingleton): Unit = {
        val wvp = langConfig.webviewViewProvider.asInstanceOf[LspSprottyViewProvider]
        wvp.openDiagram(document.uri).toFuture.onComplete {
              case Success(_) => println("Diagram has been refreshed.")
              case Failure(e) => println(s"Failed to refresh diagram: ${e}")
        }
  }

  def sendMessageToPatientTracker(narrativeTypes: List[Int]): Unit = {
    // Get current active editor's file name and send it to the patient tracker
    val flag = (narrativeTypes.contains(1), narrativeTypes.contains(2)) match {
      case (true, true)   => "12"
      case (true, false)  => "1"
      case (false, true)  => "2"
      case (false, false) => "0"
    }
    val editor = vscode.window.activeTextEditor
    editor.foreach { ed =>
      val document = ed.document
      val path = js.Dynamic.global.require("path")
      val fileName = path.basename(document.fileName).asInstanceOf[String] // Extract the file name
      val unitNumber = fileName.split("\\.").head
      patientsPanel match {
        case Some(p) =>
          p.reveal(null, preserveFocus = true)
          val req = Request(MessagingCommands.UpdateNarratives, UpdateNarratives(
            source = "vscode-extension",
            unitNumber = unitNumber,
            flag = flag
          ))
          p.webview.postMessage(req.data.toJsObject(req.command))
          vscode.window.showInformationMessage(s"Message sent to Patient Tracker: $unitNumber")
          // Ask the webview to refresh just this patient row
          p.webview.postMessage(js.Dynamic.literal(
            "command"    -> "ReloadPatient",
            "source"     -> "vscode-extension",
            "unitNumber" -> unitNumber
          ))

        case None =>
          vscode.window.showWarningMessage("Patient Panel not found, message will not be sent.")
      }
    }
  }

  def hideNarrs(langConfig: LanguageClientConfigSingleton): js.Function1[Any, Any] = {
    (args) => {
      performActionOnActivePCM(langConfig, hideNarratives)
    }
  }

  def hideNamedGroups(langConfig: LanguageClientConfigSingleton): js.Function1[Any, Any] = {
    (args) => {
      performActionOnActivePCM(langConfig, hideNGOs)     
    }
  }

  def performActionOnActivePCM(langConfig: LanguageClientConfigSingleton, f: (typings.auroraLangium.distTypesSrcLanguageGeneratedAstMod.PCM, LanguageClientConfigSingleton) => Unit): Unit = {
    val activeEditor = vscode.window.activeTextEditor
    if (activeEditor != null && activeEditor != js.undefined) {
      val t = activeEditor.asInstanceOf[TextEditor]
      parse(t.document.uri.fsPath).toFuture.onComplete {
        case Success(value) => f(value, langConfig)
        case Failure(e) => println(e)
      }
    } else { 
      println("No active text editor found.")
    }  
  }
