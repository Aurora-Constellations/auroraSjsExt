import typings.vscode.mod as vscode
import vscode.{ExtensionContext}
import scala.scalajs.js
import typings.vscode.anon.Dispose
import scala.util.*
import concurrent.ExecutionContext.Implicits.global
import com.axiom.patientTracker.PatientsListHtml.getPatientsListHtml
import docere.sjsast.*
import cats.implicits.toShow
import cats.syntax.all.toShow
import docere.sjsast.toShow
import cats.syntax.show.toShow
import com.axiom.MergePCM.MergePCM.*
import com.axiom.AuroraFile.{handleCreate, handleOpen}
import typings.sprottyVscode.libLspLspSprottyViewProviderMod.LspSprottyViewProvider
import typings.vscode.mod.TextDocument
import typings.auroraLangium.distTypesSrcExtensionLangclientconfigMod.LanguageClientConfigSingleton
import typings.vscode.mod.OutputChannel
import typings.auroraLangium.distTypesSrcExtensionSrcCommandsToggleDiagramLayoutCommandMod.toggleDiagramLayout
import com.axiom.Narratives.ManageNarratives.changeNarrativesType


object PublishCommands:
  private var patientsPanel: Option[vscode.WebviewPanel] = None // Store reference to the webview panel

  def publishCommands(context: ExtensionContext, langConfig: LanguageClientConfigSingleton): Unit = {
      val commands = List(
          ("AuroraSjsExt.aurora", showHello()),
          ("AuroraSjsExt.patients", showPatients(context)),
          ("AuroraSjsExt.processDSL", processDSL(context)),
          ("AuroraSjsExt.toggleDiagramLayout", toggleLayout(langConfig)),
          ("AuroraSjsExt.changeNarrativeType", changeNarrativesType(context))
      )

      commands.foreach { case (name, fun) =>
          context.subscriptions.push(
              vscode.commands
                  .registerCommand(name, fun)
                  .asInstanceOf[Dispose]
          )
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

  def showPatients(context: ExtensionContext): js.Function1[Any, Any] =
    (args) => {
      // Close all webview views (left-hand side views)
      vscode.commands.executeCommand("workbench.action.closeSidebar").toFuture.onComplete {
        case Success(_) => println("Closed all webview views.")
        case Failure(e) => println(s"Failed to close webview views: ${e.getMessage}")
      }
      patientsPanel match {
        case Some(panel) if !js.isUndefined(panel) =>
          panel.reveal(vscode.ViewColumn.One)
        case _ =>
          createPatientsPanel(context)
      }
    }

  
  def createPatientsPanel(context: ExtensionContext): Unit = {
    val path = js.Dynamic.global.require("path")
    val panel = vscode.window.createWebviewPanel(
      "Patients", // Internal identifier of the webview panel
      "Patient List", // Title of the panel displayed to the user
      vscode.ViewColumn.One, // Editor column to show the new webview panel in
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
    panel.webview.html = getPatientsListHtml(panel.webview, context)
    
    // Handle messages from the webview
    panel.webview.onDidReceiveMessage { (message: Any) =>
      // Since we're using a general 'Any' type, we can cast to a js.Dynamic safely here
      val msg = message.asInstanceOf[js.Dynamic]
      val command = msg.command.asInstanceOf[String]
      val response = msg.filename.asInstanceOf[String]

      // Handle the message based on command
      command match {
        case "createAuroraFile" =>
          vscode.window.showInformationMessage(s"Creating file: $response")
          // Call the function to create the file
          handleCreate(response)
        
        case "addedToDB" =>
          vscode.window.showInformationMessage(s"Added to Database: $response")

        case "openAuroraFile" =>
          vscode.window.showInformationMessage(s"Opening file: $response")
          // Call the function to open the file
          handleOpen(response)

        case "updatedNarratives" =>
          vscode.window.showInformationMessage(s"$response")

        case other =>
          vscode.window.showWarningMessage(s"Unknown command: $other")
      }
    }

    // Handle disposal
    panel.onDidDispose((_: Unit) => { // Changed the lambda to accept a Unit argument
      println("Patient panel disposed.")
      patientsPanel = None // Reset the panel reference
    })

    // Store the panel reference and handle disposal
    patientsPanel = Some(panel)
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

  def sendMessageToPatientTracker(): Unit = {
    patientsPanel match {
      case Some(p) =>
        p.reveal(null, preserveFocus = false)
        p.webview.postMessage(js.Dynamic.literal(
          command = "updateNarratives",
          payload = js.Dynamic.literal(draft = true, 
            urgent = true,
            normal = false
          )
        ))
        vscode.window.showInformationMessage("Message sent to Patient Tracker.")
      case None =>
        vscode.window.showWarningMessage("Patient Panel not found, updates will not be sent.")
    }
  }
