package com.axiom.audio

import scala.scalajs.js
import typings.vscode.mod.ExtensionContext

object AudioToTextCommands:

    private var backendProc: js.Dynamic = _

    def ensureBackendRunning(context: ExtensionContext): Unit =
        if backendProc == null then
            val cp = js.Dynamic.global.require("child_process")
            val isWindows = js.Dynamic.global.process.platform.toString == "win32"
            val exe = if isWindows then "audiototext.bat" else "audiototext"
            val path = s"${context.extensionPath}/audiototext/target/pack/bin/$exe"
            println(s"Starting backend process: $path")
            js.Dynamic.global.console.log(s"Spawning backend with: path=$path, isWindows=$isWindows")
            val spawnOptions = js.Dynamic.literal(
                stdio = js.Array("pipe", "pipe", "pipe"),
                cwd = context.extensionPath  // <-- set working directory here
            )
            if (isWindows) {
                backendProc = cp.spawn("cmd.exe", js.Array("/c", path), spawnOptions)
            } else {
                backendProc = cp.spawn(path, js.Array(), spawnOptions)
            }

            backendProc.stdout.on("data", (data: js.Any) =>
                js.Dynamic.global.console.log(s"Backend: ${data.toString}")
            )

            backendProc.stderr.on("data", (data: js.Any) =>
                js.Dynamic.global.console.error(s"Backend error: ${data.toString}")
            )

            backendProc.on("close", (code: js.Any) =>
                println(s"Backend process exited with code: $code")
            )

    def runBackendCommand(context: ExtensionContext, args: String*): Unit =
        ensureBackendRunning(context)
        val cmdStr = args.mkString(" ") + "\n"
        backendProc.stdin.write(cmdStr)
