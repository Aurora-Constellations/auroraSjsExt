import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt._

object Dependencies {


  /**
    * Unless you have some very, very specific and unusual requirements, this is the optimal 
    * ExecutionContext implementation for use in any Scala.js project. If you're using ExecutionContext 
    * and not using this project, you likely have some serious bugs and/or performance issues waiting to be discovered.
    */
  val scalajsmacrotaskexecutor =  Def.setting {
    Seq("org.scala-js" %%% "scala-js-macrotask-executor" % DependencyVersions.scalajsmacrotaskexecutor
    )
  }
  val scalajsdom  = Def.setting {
    Seq("org.scala-js" %%% "scalajs-dom" % DependencyVersions.scalajsdom
    )
  }
  val shapeless3 = Def.setting {
    Seq("org.typelevel" %%% "shapeless3-deriving" % DependencyVersions.shapeless3)
  }


  val aurorajslibs = Def.setting {
    Seq(
       "com.axiom" %%% "dataimportcsv3s" % "0.0.1-SNAPSHOT"
    )
  }


  //NOte I could not get scalatest to work in this project.  It seems to relate to the vscode dependency
  //try to get testing done on its dependencies instead.
  val scalatest   :     Def.Initialize[Seq[ModuleID]] = Def.setting {
    Seq(
      "org.scalactic" %%% "scalactic"  % DependencyVersions.scalatest,
      "org.scalatest" %%% "scalatest" % DependencyVersions.scalatest % "test"
    )
  }


  val laminar: Def.Initialize[Seq[ModuleID]] = Def.setting {
    Seq(
      "com.raquo" %%% "laminar" % DependencyVersions.laminar,
      "io.laminext" %%% "fetch" % DependencyVersions.laminext,
    )
  }

  //json codec library
  val borerJson: Def.Initialize[Seq[ModuleID]] = Def.setting {
    Seq(
      "io.bullet" %%% "borer-core" % DependencyVersions.borerjson,
      "io.bullet" %%% "borer-derivation" % DependencyVersions.borerjson
    )
  }

}
