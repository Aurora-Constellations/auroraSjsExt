package com.axiom.audio.node

import scala.scalajs.js
import scala.scalajs.js.annotation.*
import typings.vscode.mod.ExtensionContext
import scala.concurrent.{Future, Promise}

// Facade for child_process.spawn
@js.native
@JSImport("child_process", "spawn")
object spawn extends js.Function3[String, js.Array[String], SpawnOptions, ChildProcess] {
	def apply(
		command: String,
		args: js.Array[String] = js.Array(),
		options: SpawnOptions = new js.Object().asInstanceOf[SpawnOptions]
	): ChildProcess = js.native
}

@js.native
trait SpawnOptions extends js.Object:
	var stdio: js.UndefOr[js.Array[String]] = js.native
	var cwd: js.UndefOr[String] = js.native

object SpawnOptions:
	def apply(
		stdio: js.Array[String] = js.Array("pipe", "pipe", "pipe"),
		cwd: js.UndefOr[String] = js.undefined
	): SpawnOptions =
		js.Dynamic.literal(
			stdio = stdio,
			cwd = cwd
		).asInstanceOf[SpawnOptions]

// Facade for ChildProcess
@js.native
trait ChildProcess extends js.Object:
	val stdin: NodeWritable = js.native
	val stdout: NodeReadable = js.native
	val stderr: NodeReadable = js.native

	def on(event: String, listener: js.Function1[js.Any, Unit]): Unit = js.native
	def off(event: String, listener: js.Function1[js.Any, Unit]): Unit = js.native

@js.native
trait NodeWritable extends js.Object:
	def write(chunk: String): Boolean = js.native

@js.native
trait NodeReadable extends js.Object:
	def on(event: String, listener: js.Function1[js.Any, Unit]): Unit = js.native
	def off(event: String, listener: js.Function1[js.Any, Unit]): Unit = js.native