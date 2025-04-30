import typings.vscode.mod as vscode
import vscode.{ExtensionContext}
import scala.scalajs.js
import typings.vscode.anon.Dispose
import scala.util.*
import concurrent.ExecutionContext.Implicits.global
import com.axiom.patientTracker.PatientsListHtml.getPatientsListHtml
import docere.sjsast.*
export typings.auroraLangium.cliMod.parse
import cats.implicits.toShow
import cats.syntax.all.toShow
import docere.sjsast.toShow
import cats.syntax.show.toShow

object PublishCommands:
  private var patientsPanel: Option[vscode.WebviewPanel] = None // Store reference to the webview panel

  def publishCommands(context: ExtensionContext): Unit = {
      val commands = List(
          ("AuroraSjsExt.aurora", showHello()),
          ("AuroraSjsExt.patients", showPatients(context)),
          ("AuroraSjsExt.mergepcm", mergePCM(context))
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
    // Handle disposal
    panel.onDidDispose((_: Unit) => { // Changed the lambda to accept a Unit argument
      println("Patient panel disposed.")
      patientsPanel = None // Reset the panel reference
    })

    // Store the panel reference and handle disposal
    patientsPanel = Some(panel)
  }

  def mergePCM(context: ExtensionContext): js.Function1[Any, Any] =
    (args) => {
      //context |+| context
      mergeissues(context)
    }

def prettyPrint(pcm: PCM): String = {
  val sb = new StringBuilder

  // Print issues
  pcm.cio.get("Issues") match {
    case Some(issues: Issues) =>
      sb.append("Issues: ")
      if issues.narrative.nonEmpty then
        val narratives = issues.narrative.map(_.name).toList.sorted
        sb.append(narratives.mkString(" "))
        sb.append("\n")
      issues.ics.toList.sortBy(_.name).foreach { ic =>
      val narrativeStr = 
        if (ic.narrative.nonEmpty)
          " " + ic.narrative.map(_.name).mkString("; ")
        else ""
      sb.append(s"${ic.name}$narrativeStr\n")
    }
      sb.append("\n")
    case _ =>
  }

  // Print orders
  pcm.cio.get("Orders") match {
    case Some(orders: Orders) =>
      sb.append("Orders:\n")
      orders.ngo.foreach { ngo =>
        sb.append(s"${ngo.name}\n")
        ngo.orderCoordinates.foreach { oc =>
          val refsStr = oc.refs.map(_.name).mkString(",")
          if (refsStr.nonEmpty)
            sb.append(s"${oc.name}($refsStr) \n")
          else
            sb.append(s"${oc.name} \n")
        }
        sb.append("\n")
      }
    case _ =>
  }

  sb.toString()
}

def mergeissues(context: ExtensionContext): Unit = {
  val fs = js.Dynamic.global.require("fs")
  val path = js.Dynamic.global.require("path")

  for {
    file1Uris <- vscode.window.showOpenDialog(
      js.Dynamic.literal(
        canSelectMany = false,
        openLabel = "Select the first file"
      ).asInstanceOf[vscode.OpenDialogOptions]
    ).toFuture
    file2Uris <- vscode.window.showOpenDialog(
      js.Dynamic.literal(
        canSelectMany = false,
        openLabel = "Select the second file"
      ).asInstanceOf[vscode.OpenDialogOptions]
    ).toFuture
  } {
    (file1Uris.toOption, file2Uris.toOption) match {
    case (Some(file1Uris), Some(file2Uris)) if file1Uris.length > 0 && file2Uris.length > 0 =>
      val file1 = file1Uris(0).asInstanceOf[vscode.Uri]
      val file2 = file2Uris(0).asInstanceOf[vscode.Uri]

      val file1Path = file1.fsPath.trim
      val file2Path = file2.fsPath.trim

      val fs = js.Dynamic.global.require("fs")
      try {
        //val file1Content = fs.readFileSync(file1Path, "utf8").asInstanceOf[String]
        //val file2Content = fs.readFileSync(file2Path, "utf8").asInstanceOf[String]
        for {
          p1 <-  parse(file1Path.asInstanceOf[String]).toFuture  
          p2 <-  parse(file2Path.asInstanceOf[String]).toFuture
          }
        yield {
            val result = (PCM(p1)).merge(PCM(p2))
            
            val merged_results = prettyPrint(result)
            println("printing output")
            println(result)
            val examplesPath = path.join(context.extensionPath, "examples").toString
              fs.mkdirSync(examplesPath)


            val outputFilePath = path.join(examplesPath, "merged_output.aurora").toString

            // Write the result to the file
            fs.writeFileSync(outputFilePath, merged_results, "utf8")

            // Notify the user
            vscode.window.showInformationMessage(s"Result saved to $outputFilePath")
        }
        //fs.writeFileSync(file2Path, mergedContent, "utf8")

        vscode.window.showInformationMessage("Files merged successfully.")
      } catch {
        case e: Throwable =>
          vscode.window.showErrorMessage("Failed to merge files: " + e.getMessage())
      }
    case _ =>
      vscode.window.showWarningMessage("Both files must be selected.")
  }
}
}
