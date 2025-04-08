import scala.sys.process._

lazy val installDependencies = Def.task[Unit] {
  val base = (ThisProject / baseDirectory).value
  val log = (ThisProject / streams).value.log
  if (!(base / "node_module").exists) {
    val pb =
      new java.lang.ProcessBuilder("npm.cmd", "install")
        .directory(base)
        .redirectErrorStream(true)

    pb ! log
  }
}

lazy val open = taskKey[Unit]("open vscode")
def openVSCodeTask: Def.Initialize[Task[Unit]] =
  Def
    .task[Unit] {
      val base = baseDirectory.value
      val log = streams.value.log

      val path = base.getCanonicalPath
      s"code.cmd --extensionDevelopmentPath=$path" ! log
      ()
    }
    // .dependsOn(installDependencies)

lazy val root = project
  .in(file("."))
  .settings(
    scalaVersion := DependencyVersions.scala,
    moduleName := "auroraSjsExt",
    Compile / fastOptJS / artifactPath := baseDirectory.value / "out" / "extension.js",
    Compile / fullOptJS / artifactPath := baseDirectory.value / "out" / "extension.js",
    open := openVSCodeTask.dependsOn(Compile / fastOptJS).value,
        // CommonJS
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
    libraryDependencies ++= Dependencies.scalatest.value,

    // Compile / npmDependencies ++= Seq("@types/vscode" -> "1.84.1"),
    // Tell ScalablyTyped that we manage `npm install` ourselves
    externalNpm := baseDirectory.value,

    testFrameworks += new TestFramework("utest.runner.Framework")
    // publishMarketplace := publishMarketplaceTask.dependsOn(fullOptJS in Compile).value
  )
  .enablePlugins(
    ScalaJSPlugin,
    ScalablyTypedConverterExternalNpmPlugin
    // ScalablyTypedConverterPlugin
  )
