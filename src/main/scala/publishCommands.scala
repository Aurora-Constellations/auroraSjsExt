import typings.vscode.mod as vscode
import vscode.{ExtensionContext}
import scala.scalajs.js
import typings.vscode.anon.Dispose
import scala.util.*
import concurrent.ExecutionContext.Implicits.global
import com.axiom.patienttracker.showPatients
import com.axiom.billing.showBilling
import docere.sjsast.*
import cats.implicits.toShow
import cats.syntax.all.toShow
import docere.sjsast.toShow
import cats.syntax.show.toShow
import com.axiom.MergePCM.MergePCM.*
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
import com.axiom.mcp.ClaudeClient
import com.axiom.mcp.McpHandler
import com.axiom.audio.AudioToTextCommands
import scala.concurrent.Future
import scala.scalajs.js.annotation.JSImport
import scala.compiletime.uninitialized
import scala.scalajs.js.timers.{SetIntervalHandle, setInterval, clearInterval}

object PublishCommands:
  private var recordingItem: vscode.StatusBarItem = uninitialized
  private var isRecording: Boolean = false
  private var startTime: Double = 0.0
  private var timerHandle: SetIntervalHandle | Null = null

  def initRecordingStatusBar(context: vscode.ExtensionContext): Unit = {
    recordingItem = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Left, 100)
    context.subscriptions.push(recordingItem.asInstanceOf[Dispose])
    updateRecordingStatusBar()
  }

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
          ("AuroraSjsExt.mcp", takeMcpPrompt(context)),
          ("AuroraSjsExt.startRecording", startRecording(context)),
          ("AuroraSjsExt.stopRecording", stopRecording(context)),
          ("AuroraSjsExt.transcribeRecording", transcribeAudio(context)),
          ("AuroraSjsExt.transcribeAndRunMcp", transcribeAndRunMCP(context))
      )

      commands.foreach { case (name, fun) =>
          context.subscriptions.push(
              vscode.commands
                  .registerCommand(name, fun)
                  .asInstanceOf[Dispose]
          )
      }
  }

  def startRecording(context: ExtensionContext): js.Function1[Any, Future[Unit]] = { _ =>
    val outPath = s"${context.extensionPath}/recordings/latest.wav"
    AudioToTextCommands.runBackendCommand(context, "record", outPath).flatMap{_ => 
      isRecording = true
      startTime = js.Date.now()
      startTimer()
      updateRecordingStatusBar()
      vscode.window.showInformationMessage("Recording started. Click 'Stop Recording' to end.")
        .toFuture.map(_ => ())
    }
  }

  def stopRecording(context: ExtensionContext): js.Function1[Any, Future[Unit]] = { _ =>
    AudioToTextCommands.runBackendCommand(context, "stop").flatMap { _ =>
      isRecording = false
      stopTimer()
      updateRecordingStatusBar()
      vscode.window.showInformationMessage("Recording stopped. Transcribing...")
        .toFuture
        .flatMap { _ =>
          transcribeAndRunMCP(context)(())
        }
    }
  }

  private def startTimer(): Unit = {
    stopTimer() // Ensure any existing timer is cleared
    timerHandle = setInterval(1000) {
      if (isRecording) {
        val elapsedSeconds = ((js.Date.now() - startTime) / 1000).toInt
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60
        recordingItem.text = f"$$(stop-circle) Stop Recording (${minutes}%02d:${seconds}%02d)"
      }
    }
  }

  private def stopTimer(): Unit = {
    if (timerHandle != null) {
      clearInterval(timerHandle)
      timerHandle = null
    }
  }

  def updateRecordingStatusBar(): Unit = {
    if (isRecording) {
      recordingItem.text = "$(stop-circle) Stop Recording"
      recordingItem.command = "AuroraSjsExt.stopRecording"
      recordingItem.color = "#FF5555" //red color to indicate recording
    } else {
      recordingItem.text = "$(play) Start Recording"
      recordingItem.command = "AuroraSjsExt.startRecording"
      recordingItem.color = "#55FF55" //green color to indicate not recording
    }
    recordingItem.show()
  }

  def transcribeAudio(context: ExtensionContext): js.Function1[Any, Future[Unit]] = { _ =>
    AudioToTextCommands.runBackendCommand(context, "transcribe", s"${context.extensionPath}/recordings/latest.wav")
  }

  def transcribeAndRunMCP(context: ExtensionContext): js.Function1[Any, Future[Unit]] = { _ =>
    val transcriptionFuture: Future[String] =
      transcribeAudio(context)(()).flatMap { _ =>
        fsPromises
          .readFile(s"${context.extensionPath}/recordings/latest_transcription.txt", "utf8")
          .toFuture
      }

    transcriptionFuture
      .flatMap(transcription => ClaudeClient.getMcpFromPrompt(transcription))
      .map { mcpJson =>
        vscode.window.showInformationMessage(s"Performing the task....")
        val mcpString = js.JSON.stringify(mcpJson)
        McpHandler.action(mcpString)
      }
      .map(_ => ())
      .recover {
        case e: Throwable =>
          val errMsg = s"Error in transcription or Claude API: ${e.getMessage}"
          vscode.window.showErrorMessage(errMsg)
        ()
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

  // Node.js filesystem module
  @js.native
  @JSImport("fs", "promises")
  object fsPromises extends js.Object {
    def readFile(path: String, encoding: String = "utf8"): js.Promise[String] = js.native
  }