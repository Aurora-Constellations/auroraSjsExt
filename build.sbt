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

def copyJSCSS(mediaDir:File, outputDir: File, cssFile:File, jsFileName: String, cssFileName: String): String = {
  IO.createDirectory(mediaDir)
  // Copy JS files
  val jsFiles = (outputDir ** "*.js").get
  jsFiles.foreach { file =>
    val target = mediaDir / jsFileName
    IO.copyFile(file, target, preserveLastModified = true)
  }
  // Copy styles.css
  if (cssFile.exists()) {
    val cssTarget = mediaDir / cssFileName
    IO.copyFile(cssFile, cssTarget, preserveLastModified = true)
    return s"Copied ${cssFileName} & ${jsFileName} to media/"
  } else {
    return s"${cssFileName} not found"
  }
}


// --- Custom Task: Copy Scala.js output to media ---
lazy val copyToMedia = Def.task[Unit] {
  val log = streams.value.log
  val base = baseDirectory.value
  val mediaDir = base / "media"
  val outputDir_patienttracker = (axiompatienttracker / Compile / fastLinkJS / scalaJSLinkerOutputDirectory).value
  val outputDir_billing = (axiombilling / Compile / fastLinkJS / scalaJSLinkerOutputDirectory).value

  val cssFile_patienttracker = base / "axiompatienttracker" / "src" / "styles.css"
  val cssFile_billing = base / "axiombilling" / "src" / "styles.css"

  

  log.info(copyJSCSS(mediaDir, outputDir_patienttracker, cssFile_patienttracker, "main.js", "styles.css"))
  log.info(copyJSCSS(mediaDir, outputDir_billing, cssFile_billing, "ab_main.js", "ab_styles.css"))
  
}

lazy val createDirectories = Def.task[Unit] {
  val base = baseDirectory.value
  val log = streams.value.log
  val recordingsDir = base / "recordings"
  val auroraFilesDir = base / "auroraFiles"

  def createDir(dir: File): Unit = {
    if (!dir.exists()) {
      IO.createDirectory(dir)
      log.info(s"Created directory: ${dir.getAbsolutePath}")
    } else {
      log.info(s"Directory already exists: ${dir.getAbsolutePath}")
    }
  }

  createDir(recordingsDir)
  createDir(auroraFilesDir)
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
      .dependsOn(audioToText / Compile/ compile)
      .dependsOn(audioToText / Compile / pack)
      .dependsOn(axiompatienttracker / Compile / fastLinkJS)
      .dependsOn(axiombilling / Compile / fastLinkJS)
      .dependsOn(copyToMedia)
      .dependsOn(installDependencies)
      .dependsOn(createDirectories)
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
    libraryDependencies ++= Dependencies.sttpClient4.value,
    libraryDependencies ++= Dependencies.circe.value

  )

// --- Axiom Billing Frontend (Scala.js) ---
lazy val axiombilling = project
  .in(file("axiombilling"))
  .enablePlugins(ScalaJSPlugin, ScalablyTypedConverterExternalNpmPlugin)
  .dependsOn(shared.js)
  .settings(
    name := "axiombilling",
    scalaJSUseMainModuleInitializer := true,
    scalacOptions ++= Seq("-Yretain-trees", "-Xmax-inlines", "60","-explain"),
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(ModuleSplitStyle.SmallModulesFor(List("axiombilling")))
    },
    externalNpm := baseDirectory.value,
    resolvers += "Artima Maven Repository" at "https://repo.artima.com/releases",
    libraryDependencies ++= Dependencies.scalajsdom.value,
    libraryDependencies ++= Dependencies.scalajsmacrotaskexecutor.value,
    libraryDependencies ++= Dependencies.laminar.value,
    libraryDependencies ++= Dependencies.scalatest.value,
    libraryDependencies ++= Dependencies.aurorajslibs.value,
    libraryDependencies ++= Dependencies.shapeless3.value
  )

// --- Shared Cross-Project (shared between frontend + backend) ---
lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .in(file("shared"))
  .settings(
    libraryDependencies ++= Dependencies.borerJson.value,
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio-json" % DependencyVersions.zioJson, // ✅ ensure zio-json is available here
      "org.typelevel" %%% "shapeless3-deriving" % "3.3.0"
    )
  )
  .jvmSettings(
    libraryDependencies += "org.scala-js" %% "scalajs-stubs" % DependencyVersions.scalaJsStubs
  )
  .jsSettings(
    libraryDependencies ++= Dependencies.laminar.value 
  )


// --- Audio To Text ---
lazy val audioToText = project
  .in(file("audiototext"))
  .enablePlugins(PackPlugin)
  .settings(
    name := "audiototext",
    // mainClass := Some("com.axiom.audio.Main"),
    packMain := Map("audiototext" -> "com.axiom.audio.Main"), 
    libraryDependencies ++= Dependencies.betterfiles.value,
    libraryDependencies ++= Dependencies.sttpClient4.value,
    libraryDependencies ++= Dependencies.circe.value,
    libraryDependencies ++= Dependencies.cats.value,
    libraryDependencies ++= Dependencies.config.value
  )