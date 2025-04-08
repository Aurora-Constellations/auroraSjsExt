import typings.vscode.mod as vscode

import scala.collection.immutable
import vscode.{WebviewPanelOptions, WebviewOptions}

object MyWebViewPanel:
    // Create a new panel.
    def createNewPanel(viewType: String, title: String, column: vscode.ViewColumn, extensionUri: vscode.Uri) =
        val localResourceRoots = immutable.Seq(vscode.Uri.joinPath(extensionUri, "media"))
        vscode.window.createWebviewPanel(
            viewType,
            title,
            column,
            new WebviewOptions{
                override val enableScripts = true
                override val localResourceRoots = localResourceRoots
            }.asInstanceOf[WebviewOptions & WebviewPanelOptions]
        );
