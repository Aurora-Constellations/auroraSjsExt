import typings.vscode.mod as vscode
import vscode.{ExtensionContext}
import scala.scalajs.js
import typings.vscode.anon.Dispose
import scala.util.*
import concurrent.ExecutionContext.Implicits.global

object PublishCommands:
    def publishCommands(context: ExtensionContext): Unit = {
        val commands = List(
            ("AuroraSjsExt.aurora", showHello())
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