import scala.sys.process._
import org.scalajs.linker.interface.{ModuleSplitStyle, ModuleKind}
import org.scalajs.jsenv.nodejs.NodeJSEnv

// Common Settings
ThisBuild / organization := "com.axiom"
ThisBuild / version := "0.0.1"
ThisBuild / scalaVersion := DependencyVersions.scala

// Shared ScalablyTyped Settings
// Points all projects to the root node_modules and ignores problematic types
val sharedStSettings = Seq(
  externalNpm := (ThisBuild / baseDirectory).value,
  stIgnore ++= Seq("node", "typescript").toList
)

// --- Custom Task: Install Dependencies ---
lazy val installDependencies = Def.task[Unit] {
  val rootBase = (ThisBuild / baseDirectory).value
  val log = streams.value.log
  
  if (!(rootBase / "node_modules").exists()) {
    log.info("Installing monorepo dependencies at root...")
    val isWindows = System.getProperty("os.name").toLowerCase.contains("win")
    val npmCommand = if (isWindows) "npm.cmd" else "npm"
    Process(Seq(npmCommand, "install"), rootBase) ! log
  }

  val auroraLangiumDir = rootBase / "node_modules" / "aurora-langium"
  val base = baseDirectory.value 

  def copyDir(src: File, dest: File): Unit = {
    if (src.exists && src.isDirectory) {
      IO.copyDirectory(src, dest)
      log.info(s"Copied ${src.getName} to ${dest.getAbsolutePath}")
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
  .dependsOn(axiompatienttracker, pcmalgebra)
  .settings(
    name := "auroraSjsExt",
    open := openVSCodeTask.dependsOn(Compile / fastOptJS).value,
    scalacOptions ++= Seq("-Xmax-inlines", "100"),
    Test / jsEnv := new NodeJSEnv(
      NodeJSEnv.Config().withEnv(
        Map("NODE_PATH" -> (baseDirectory.value / "node_modules").getAbsolutePath)
      )
    ),
    Compile / fastOptJS := (Compile / fastOptJS)
      .dependsOn(audioToText / Compile/ compile)
      .dependsOn(audioToText / Compile / pack)
      .dependsOn(axiompatienttracker / Compile / fastLinkJS)
      .dependsOn(axiombilling / Compile / fastLinkJS)
      .dependsOn(pcmalgebra / Compile / fastLinkJS)
      .dependsOn(d3example / Compile / fastLinkJS)
      .dependsOn(copyToMedia)
      .dependsOn(installDependencies)
      .dependsOn(createDirectories)
      .value,
    Compile / fastOptJS / artifactPath := baseDirectory.value / "out" / "extension.js",
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
    libraryDependencies ++= Dependencies.scalatest.value,
    libraryDependencies ++= Dependencies.cats.value,
    testFrameworks += new TestFramework("utest.runner.Framework")
  )
  .settings(sharedStSettings)

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
  .settings(sharedStSettings)

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
    resolvers += "Artima Maven Repository" at "https://repo.artima.com/releases",
    libraryDependencies ++= Dependencies.scalajsdom.value,
    libraryDependencies ++= Dependencies.scalajsmacrotaskexecutor.value,
    libraryDependencies ++= Dependencies.laminar.value,
    libraryDependencies ++= Dependencies.scalatest.value,
    libraryDependencies ++= Dependencies.aurorajslibs.value,
    libraryDependencies ++= Dependencies.shapeless3.value
  )
  .settings(sharedStSettings)

// --- Shared Cross-Project (shared between frontend + backend) ---
lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .in(file("shared"))
  .settings(
    libraryDependencies ++= Dependencies.borerJson.value,
    libraryDependencies ++= Dependencies.shapeless3.value,
    libraryDependencies ++= Dependencies.laminar.value,
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio-json" % DependencyVersions.zioJson, // ✅ ensure zio-json is available here
    )
  )
  .jvmSettings(
    libraryDependencies += "org.scala-js" %% "scalajs-stubs" % DependencyVersions.scalaJsStubs
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

// --- PCM Algebra ---
lazy val pcmalgebra = project
  .in(file("pcmalgebra"))
  .enablePlugins(ScalaJSPlugin) // Enable the Scala.js plugin in this project
  .enablePlugins(ScalablyTypedConverterExternalNpmPlugin)
  .settings(
    name := "pcmalgebra",
    scalaVersion := DependencyVersions.scala,
    scalacOptions ++= Seq("-Yretain-trees", "-Xmax-inlines", "60","-explain"),
    Test / resourceDirectory := baseDirectory.value / "src" / "test" / "resources",
    /* Configure Scala.js to emit modules in the optimal way to
     * connect to Vite's incremental reload.
     * - emit ECMAScript modules
     * - emit as many small modules as possible for classes in the "livechart" package
     * - emit as few (large) modules as possible for all other classes
     *   (in particular, for the standard library)
     */
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(
          ModuleSplitStyle.SmallModulesFor(List("pcmalgebra")))
    },

    /*
     *add resolver for scalatest
     */
    resolvers += "Artima Maven Repository" at "https://repo.artima.com/releases",


    /* Depend on the scalajs-dom library.
     * It provides static types for the browser DOM APIs.
     */
    libraryDependencies ++= Dependencies.scalajsdom.value,
    libraryDependencies ++= Dependencies.laminar.value,
    libraryDependencies ++= Dependencies.upickle.value,
    libraryDependencies ++= Dependencies.scalatest.value,
    libraryDependencies +="org.scala-js" %%% "scala-js-macrotask-executor" % "1.1.1",
    libraryDependencies ++= Dependencies.cats.value,
    libraryDependencies ++= Dependencies.magnolia.value,
  )
  .settings(sharedStSettings)

lazy val scorepcmcli = project
  .in(file("pcmalgebra/cli"))
  .enablePlugins(ScalaJSPlugin, ScalablyTypedConverterExternalNpmPlugin)
  .dependsOn(pcmalgebra)
  .settings(
    name := "scorepcmcli",
    scalaVersion := DependencyVersions.scala,
    scalaJSUseMainModuleInitializer := true,
    scalacOptions ++= Seq("-Yretain-trees", "-Xmax-inlines", "60", "-explain"),
    resolvers += "Artima Maven Repository" at "https://repo.artima.com/releases",
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.CommonJSModule)
        .withModuleSplitStyle(ModuleSplitStyle.FewestModules)
    },
    Compile / fastOptJS / artifactPath := baseDirectory.value / "score-pcm.cjs",
    libraryDependencies ++= Dependencies.scalatest.value,
    libraryDependencies ++= Dependencies.upickle.value
  )
  .settings(sharedStSettings)

// --- D3 Example ---
lazy val d3example = project
  .in(file("d3example"))
  .enablePlugins(ScalaJSPlugin, ScalablyTypedConverterExternalNpmPlugin)
  .settings(
    name := "d3example",
    scalaJSUseMainModuleInitializer := true,
    scalacOptions ++= Seq("-encoding", "utf-8", "-deprecation", "-feature"),
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
       .withModuleSplitStyle(ModuleSplitStyle.FewestModules)
    },
    resolvers += "Artima Maven Repository" at "https://repo.artima.com/releases",
    libraryDependencies ++= (
      Dependencies.scalajsdom.value ++
      Dependencies.laminar.value ++
      Dependencies.scalatest.value
    )
  )
  .settings(sharedStSettings)
