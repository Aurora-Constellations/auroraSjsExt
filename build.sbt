import scala.sys.process._
import org.scalajs.linker.interface.{ModuleSplitStyle, ModuleKind}

// Common Settings
ThisBuild / organization := "com.axiom"
ThisBuild / version := "0.0.1"
ThisBuild / scalaVersion := DependencyVersions.scala

// --- Custom Task: Install Dependencies ---
lazy val installDependencies = Def.task[Unit] {
  val base = baseDirectory.value
  val log = streams.value.log
  val nodeModulesDir = base / "node_modules"
  val auroraLangiumDir = nodeModulesDir / "aurora-langium"

  if (!nodeModulesDir.exists()) {
    val isWindows = System.getProperty("os.name").toLowerCase.contains("win")
    val npmCommand = if (isWindows) "npm.cmd" else "npm"
    val pb = new java.lang.ProcessBuilder(npmCommand, "install")
      .directory(base)
      .redirectErrorStream(true)
    pb ! log
  }

  def copyDir(src: File, dest: File): Unit = {
    if (src.exists && src.isDirectory) {
      IO.copyDirectory(src, dest)
      log.info(s"Copied ${src.getName} to ${dest.getAbsolutePath}")
    } else {
      log.warn(s"Directory ${src.getAbsolutePath} does not exist!")
    }
  }

  copyDir(auroraLangiumDir / "pack", base / "pack")
  copyDir(auroraLangiumDir / "syntaxes", base / "syntaxes")
}

// --- Custom Task: Copy Scala.js output to media ---
lazy val copyToMedia = Def.task[Unit] {
  val log = streams.value.log
  val base = baseDirectory.value
  val outputDir = (axiompatienttracker / Compile / fastLinkJS / scalaJSLinkerOutputDirectory).value
  val mediaDir = base / "media"
  val cssFile = base / "axiompatienttracker" / "src" / "styles.css"

  IO.createDirectory(mediaDir)

  // Copy JS files
  val jsFiles = (outputDir ** "*.js").get
  jsFiles.foreach { file =>
    val target = mediaDir / file.name
    IO.copyFile(file, target, preserveLastModified = true)
    log.info(s"Copied ${file.getName} to media/")
  }

  // Copy styles.css
  if (cssFile.exists()) {
    val cssTarget = mediaDir / "styles.css"
    IO.copyFile(cssFile, cssTarget, preserveLastModified = true)
    log.info(s"Copied styles.css to media/")
  } else {
    log.warn("styles.css not found in axiompatienttracker/src/")
  }
}

// --- Custom Task: Launch VS Code Extension Host Preview ---
lazy val open = taskKey[Unit]("open vscode")
def openVSCodeTask: Def.Initialize[Task[Unit]] =
  Def
    .task[Unit] {
      val base = baseDirectory.value
      val log = streams.value.log

      val path = base.getCanonicalPath
      val isWindows = System.getProperty("os.name").toLowerCase.contains("win")

      val command = if (isWindows) "code.cmd" else "code"
      s"$command --extensionDevelopmentPath=$path" ! log
      ()
    }
    .dependsOn(copyToMedia)

// --- Root Project ---
lazy val root = project
  .in(file("."))
  .enablePlugins(ScalaJSPlugin, ScalablyTypedConverterExternalNpmPlugin)
  .dependsOn(axiompatienttracker)
  .settings(
    name := "auroraSjsExt",
    open := openVSCodeTask.dependsOn(Compile / fastOptJS).value,
    Compile / fastOptJS := (Compile / fastOptJS)
      .dependsOn(axiompatienttracker / Compile / fastLinkJS)
      .dependsOn(copyToMedia)
      .dependsOn(installDependencies)
      .value,
    Compile / fastOptJS / artifactPath := baseDirectory.value / "out" / "extension.js",
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
    externalNpm := baseDirectory.value,
    libraryDependencies ++= Dependencies.scalatest.value,
    libraryDependencies ++= Dependencies.cats.value,

    testFrameworks += new TestFramework("utest.runner.Framework")
  )

// --- Axiom Patient Tracker Frontend (Scala.js) ---
lazy val axiompatienttracker = project
  .in(file("axiompatienttracker"))
  .enablePlugins(ScalaJSPlugin, ScalablyTypedConverterExternalNpmPlugin)
  .dependsOn(shared.js)
  .settings(
    name := "axiompatienttracker",
    scalaJSUseMainModuleInitializer := true,
    scalacOptions ++= Seq("-Yretain-trees", "-Xmax-inlines", "60","-explain"),
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(ModuleSplitStyle.SmallModulesFor(List("axiompatienttracker")))
    },
    externalNpm := baseDirectory.value,
    resolvers += "Artima Maven Repository" at "https://repo.artima.com/releases",
    libraryDependencies ++= Dependencies.scalajsdom.value,
    libraryDependencies ++= Dependencies.scalajsmacrotaskexecutor.value,
    libraryDependencies ++= Dependencies.laminar.value,
    libraryDependencies ++= Dependencies.scalatest.value,
    libraryDependencies ++= Dependencies.aurorajslibs.value,
    libraryDependencies ++= Dependencies.shapeless3.value,
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %%% "core" % "3.9.0",
      "com.softwaremill.sttp.client3" %%% "circe" % "3.9.0",
      "io.circe" %%% "circe-core" % "0.14.6",
      "io.circe" %%% "circe-generic" % "0.14.6",
      "io.circe" %%% "circe-parser" % "0.14.6"
)

  )

// --- Shared Cross-Project (shared between frontend + backend) ---
lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .in(file("shared"))
  .settings(
    libraryDependencies ++= Dependencies.borerJson.value,
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio-json" % DependencyVersions.zioJson // ✅ ensure zio-json is available here
    )
  )
  .jvmSettings(
    libraryDependencies += "org.scala-js" %% "scalajs-stubs" % DependencyVersions.scalaJsStubs
  )
