package com.axiom.patients

import typings.vscode.mod as vscode
import typings.vscode.anon.Dispose
import scala.scalajs.js
import scala.util.matching.Regex
import concurrent.ExecutionContext.Implicits.global
import com.axiom.AuroraFile.handleCreate

// Sidebar list of the patient (.aurora) files in the open folder, so end users never need the file Explorer.
// Tree elements are the files' absolute paths.
class PatientsView(context: vscode.ExtensionContext) {
  private val path = js.Dynamic.global.require("path")
  private val fs = js.Dynamic.global.require("fs")

  private val changed = new vscode.EventEmitter[js.UndefOr[String]]()

  private val PatientId: Regex = "[A-Za-z0-9_-]+".r

  private def patientFolder: Option[String] =
    vscode.workspace.workspaceFolders.toOption.flatMap(_.headOption).map(_.uri.fsPath)

  private def patientFiles(): js.Array[String] =
    patientFolder match {
      case Some(dir) =>
        fs.readdirSync(dir)
          .asInstanceOf[js.Array[String]]
          .filter(_.endsWith(".aurora"))
          .sortBy(_.toLowerCase)
          .map(name => path.join(dir, name).asInstanceOf[String])
      case None => js.Array()
    }

  private def patientName(file: String): String =
    path.basename(file, ".aurora").asInstanceOf[String]

  private def isPatientFile(file: String): Boolean =
    file.endsWith(".aurora") && patientFolder.exists(dir => path.relative(dir, path.dirname(file)).asInstanceOf[String] == "")

  private val provider = js.Dynamic
    .literal(
      // The event function itself (the facade only exposes it as overloaded calls)
      onDidChangeTreeData = changed.asInstanceOf[js.Dynamic].event,
      getChildren = (element: js.UndefOr[String]) => if (element.isEmpty) patientFiles() else js.Array[String](),
      getParent = (_: String) => js.undefined,
      getTreeItem = (file: String) => {
        val item = new vscode.TreeItem(patientName(file))
        item.id = file
        item.iconPath = new vscode.ThemeIcon("person").asInstanceOf[vscode.IconPath]
        item.tooltip = file
        item.command = js.Dynamic
          .literal(
            command = "vscode.open",
            title = "Open patient",
            arguments = js.Array(vscode.Uri.file(file), js.Dynamic.literal(preview = false))
          )
          .asInstanceOf[vscode.Command]
        item
      }
    )
    .asInstanceOf[vscode.TreeDataProvider[String]]

  private val treeView = vscode.window.createTreeView[String](
    "aurora.patients",
    js.Dynamic.literal(treeDataProvider = provider).asInstanceOf[vscode.TreeViewOptions[String]]
  )

  def refresh(): Unit = changed.fire(js.undefined)

  // Marks the open patient in the list; skipped while the list is hidden, as revealing would open the sidebar
  def select(document: vscode.TextDocument): Unit =
    if (treeView.visible && isPatientFile(document.fileName))
      treeView.reveal(document.fileName, js.Dynamic.literal(select = true, focus = false).asInstanceOf[typings.vscode.anon.Expand])

  def newPatient(): js.Function1[Any, Any] = { _ =>
    vscode.window
      .showInputBox(
        js.Dynamic
          .literal(
            title = "New patient",
            prompt = "Unit number of the patient",
            placeHolder = "e.g. TB00019445",
            // An empty message means the input is valid
            validateInput = (value: String) =>
              if (PatientId.matches(value.trim)) "" else "Use letters, numbers, - and _ only"
          )
          .asInstanceOf[vscode.InputBoxOptions]
      )
      .toFuture
      .foreach { input =>
        input.toOption.map(_.trim).filter(PatientId.matches).foreach { id =>
          handleCreate(s"$id.aurora")
          refresh()
        }
      }
  }

  def register(): Unit = {
    val watcher = vscode.workspace.createFileSystemWatcher("**/*.aurora")
    watcher.onDidCreate((_: vscode.Uri) => refresh())
    watcher.onDidDelete((_: vscode.Uri) => refresh())

    context.subscriptions.push(
      treeView.asInstanceOf[Dispose],
      watcher.asInstanceOf[Dispose],
      changed.asInstanceOf[Dispose],
      vscode.commands.registerCommand("AuroraSjsExt.newPatient", newPatient()).asInstanceOf[Dispose],
      vscode.commands.registerCommand("AuroraSjsExt.refreshPatients", (_: Any) => refresh()).asInstanceOf[Dispose]
    )
  }
}
