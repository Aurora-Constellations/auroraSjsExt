import typings.vscode.mod as vscode
import vscode.{ExtensionContext}
import scala.scalajs.js
import typings.vscode.anon.Dispose
import scala.util.*
import concurrent.ExecutionContext.Implicits.global
import typings.std.stdStrings.option

object PublishCommands:
    var webviewPanel1: vscode.WebviewPanel = _
    var webviewPanel2: vscode.WebviewPanel = _

    def publishCommands(context: ExtensionContext): Unit = {
        val commands = List(
            ("AuroraSjsExt.aurora", showHello()),
            ("webview-communication.openWebview1", createWebViewPanel1(context.extensionUri)),
            ("webview-communication.openWebview2", createWebViewPanel2(context.extensionUri))
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

    def createWebViewPanel1(extensionUri: vscode.Uri): js.Function1[Any, Any] = {
        (arg) => {
            if (webviewPanel1 == null) {
                webviewPanel1 = MyWebViewPanel.createNewPanel(
                    "webview1",
                    "WebView 1",
                    vscode.ViewColumn.One,
                    extensionUri
                )
                webviewPanel1.webview.html = getWebViewHtml1()
                webviewPanel1.webview.onDidReceiveMessage(message => {
                    message match {
                        case m if m.asInstanceOf[js.Dictionary[Any]].contains("command") && m.asInstanceOf[js.Dictionary[Any]]("command") == "sendData" =>
                            val data = m.asInstanceOf[js.Dictionary[Any]]("data").asInstanceOf[String]
                            if (webviewPanel2 != null) {
                                webviewPanel2.webview.postMessage(js.Dictionary("command" -> "receiveData", "data" -> data))
                            } else {
                                println("WebView 2 is not open.")
                                vscode.window.showInformationMessage(s"WebView 2 is not open.")
                            }
                        case _ => println("Unknown message received.")
                    }
                })
            } else {
                webviewPanel1.reveal(vscode.ViewColumn.One)
            }
        }
    }

    def getWebViewHtml1() = {
        s"""
            <!DOCTYPE html>
            <html>
            <head>
            <title>Webview 1</title>
            </head>
            <body>
            <input type="text" id="dataInput" placeholder="Enter data">
            <button id="sendButton">Send</button>

            <script>
                const vscode = acquireVsCodeApi();
                const sendButton = document.getElementById('sendButton');
                const dataInput = document.getElementById('dataInput');

                sendButton.addEventListener('click', () => {
                const data = dataInput.value;
                vscode.postMessage({ command: 'sendData', data: data });
                });
            </script>
            </body>
            </html>
        """;
    }

    def createWebViewPanel2(extensionUri: vscode.Uri): js.Function1[Any, Any] = {
        (arg) => {
            if (webviewPanel2 == null) {
                webviewPanel2 = MyWebViewPanel.createNewPanel(
                    "webview2",
                    "WebView 2",
                    vscode.ViewColumn.Two,
                    extensionUri
                )
                webviewPanel2.webview.html = getWebViewHtml2()
            } else {
                webviewPanel2.reveal(vscode.ViewColumn.Two)
            }
        }
    }

    def getWebViewHtml2() = {
        s"""
            <!DOCTYPE html>
            <html>
            <head>
            <title>Webview 2</title>
            </head>
            <body>
            <div id="receivedData"></div>

            <script>
                const receivedData = document.getElementById('receivedData');

                window.addEventListener('message', event => {
                const message = event.data;
                if (message.command === 'receiveData') {
                    receivedData.textContent = 'Received: ' + message.data;
                }
                });
            </script>
            </body>
            </html>
        """;
    }