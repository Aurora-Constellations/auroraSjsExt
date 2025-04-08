import typings.vscode.mod as vscode
import typings.vscode.anon.Dispose
import scala.util.*
import scala.scalajs.js
import concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.annotation._
import vscode.{ExtensionContext}
import PublishCommands.publishCommands
import typings.auroraLangium.distTypesSrcExtensionLangclientconfigMod.LanguageClientConfigSingleton

object AuroraSjsExt {
  val langConfig = LanguageClientConfigSingleton.getInstance()

  @JSExportTopLevel("activate")
  def activate(context: vscode.ExtensionContext): Unit = {
    langConfig.setServerModule(context.asAbsolutePath("node_modules/aurora-langium/dist/cjs/language/main.cjs"))
    println(langConfig.getServerModule())
    langConfig.initialize(context)
    langConfig.registerWebviewViewProvider()
    val outputChannel = vscode.window.createOutputChannel("My Extension")
    outputChannel.appendLine("Congratulations Team Aurora, your extension 'vscode-scalajs-aurora' is now active!")
    outputChannel.show(preserveFocus = true)
    publishCommands(context)
  }

  def deactivate(): Unit = {
    langConfig.stopClient()
  }
}
