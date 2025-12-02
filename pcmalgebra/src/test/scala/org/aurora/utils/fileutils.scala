package org.aurora.utils

import scala.scalajs.js
import scala.scalajs.js.annotation.*

import scala.scalajs.js.Dynamic.global
import typings.fsExtra
import typings.auroraLangium.cliMod

@js.native
@JSImport("process", JSImport.Namespace)
private object Process extends js.Object {
  def cwd(): String = js.native
}

@js.native
@JSImport("fs", JSImport.Namespace)
object fs extends js.Object {
  def existsSync(path: String): Boolean = js.native
  def readFileSync(path: String, encoding: String): String = js.native
}

/**
  * FileReader object to read files from the file system and creates a string dsl for platorm independent paths
  */

object fileutils:
  def platform =  if (!js.isUndefined(global.process)) {
      global.process.platform.asInstanceOf[String]
    } else {
      "unknown"
    }
  val separator = platform match {
    case "win32" => "\\"
    case _ => "/"
  }

  extension (spath:String)
    def /(path: String): String = spath + separator + path



  def cwd = Process.cwd()
  def testResourcesPath = cwd / "src" / "test" / "resources"

  def readFileSync(path: String): String =
    fs.readFileSync(path, "utf-8")
  

  def createFileIfNotExists(path: String) =
    fsExtra.mod.createFileSync(path)


  def pathExists(path: String) =
    fsExtra.mod.pathExists(path)



  def parse(filename:String)  = cliMod.parse(filename)
end fileutils