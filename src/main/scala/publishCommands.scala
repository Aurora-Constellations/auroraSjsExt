import typings.vscode.mod as vscode
import vscode.{ExtensionContext}
import scala.scalajs.js
import typings.vscode.anon.Dispose
import scala.util.*
import concurrent.ExecutionContext.Implicits.global
import com.axiom.patientTracker.PatientsListHtml.getPatientsListHtml

object PublishCommands:
    def publishCommands(context: ExtensionContext): Unit = {
        val commands = List(
            ("AuroraSjsExt.aurora", showHello()),
            ("AuroraSjsExt.patients", showPatients(context))
        )

        commands.foreach { case (name, fun) =>
            context.subscriptions.push(
                vscode.commands
                    .registerCommand(name, fun)
                    .asInstanceOf[Dispose]
            )
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
      }