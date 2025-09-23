package com.axiom.audio

import scala.scalajs.js
import typings.vscode.mod.ExtensionContext
import scala.concurrent.{Future, Promise}
import com.axiom.audio.node.{spawn, ChildProcess, SpawnOptions}

object AudioToTextCommands:

	private var backendProc: ChildProcess | Null = null

	def ensureBackendRunning(context: ExtensionContext): Unit =
		if backendProc == null then
			val isWindows = js.Dynamic.global.process.platform.toString == "win32"
			val exe = if isWindows then "audiototext.bat" else "audiototext"
			val path = s"${context.extensionPath}/audiototext/target/pack/bin/$exe"
			println(s"Starting backend process: $path")

			val options = SpawnOptions(cwd = context.extensionPath)

			val proc =
			if (isWindows)
				spawn("cmd.exe", js.Array("/c", path), options)
			else
				spawn(path, js.Array(), options)

			proc.stdout.on("data", (data: js.Any) =>
				println(s"Backend: ${data.toString}")
			)

			proc.stderr.on("data", (data: js.Any) =>
				println(s"Backend error: ${data.toString}")
			)

			proc.on("close", (code: js.Any) =>
				println(s"Backend exited with code $code")
			)

			backendProc = proc

	def runBackendCommand(context: ExtensionContext, args: String*): Future[Unit] =
		ensureBackendRunning(context)

		val p = Promise[Unit]()
		val cmdStr = args.mkString(" ") + "\n"

		val proc = backendProc.nn

		var onData: js.Function1[js.Any, Unit] = null
		onData = (data: js.Any) =>
			val line = data.toString.trim
			if (line.contains("Recording started")
			|| line.contains("Recording stopped")
			|| line.contains("Transcription completed"))
			{
				p.trySuccess(())
				proc.stdout.off("data", onData)
			}

			if (line.startsWith("Transcription failed:")){ 
				p.tryFailure(new Exception(line))
				proc.stdout.off("data", onData)
			}

		proc.stdout.on("data", onData)
		proc.stdin.write(cmdStr)

		p.future
