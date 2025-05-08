package com.axiom.testutils

import scala.scalajs.js
import scala.scalajs.js.annotation.*
import scala.util.Try

import scala.scalajs.js.Dynamic.global


@js.native
@JSImport("fs", "constants")
object FSConstants extends js.Object {
  val F_OK: Int = js.native
  val R_OK: Int = js.native
  val W_OK: Int = js.native
  val X_OK: Int = js.native
}

@js.native
@JSImport("fs", JSImport.Namespace)
private object FS extends js.Object {
  val constants: js.Dynamic = js.native
  def readFileSync(path: String, encoding: String): String = js.native
  def accessSync(path: String, constant:Int): Unit = js.native
  
}

@js.native
@JSImport("process", JSImport.Namespace)
private object Process extends js.Object {
  def cwd(): String = js.native
}


/**
  * FileReader object to read files from the file system and creates a string dsl for platorm independent paths
  */

object FileUtils:
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



  def readFile(path: String): String =  FS.readFileSync(path, "utf-8")
  def cwd = Process.cwd()
  def testResourcePath = cwd / "src" / "test" / "resources"
  def testAuroraFiles = testResourcePath / "aurora" //aurora files will be placed here for testing
  def testIssueFiles = testResourcePath / "aurora" / "issues"
  def testClinicalsFiles = testResourcePath / "aurora" / "clinicals"
  def testOrdersFiles = testResourcePath / "aurora" / "orders"
  def testHelloFile = testResourcePath / "hello.txt"
  def testQUFiles = testResourcePath / "aurora" / "qu"
  
  def checkFileAccess(path: String, mode: Int = FSConstants.F_OK): Boolean = 
    Try(FS.accessSync(path, mode)).map(_ => true).getOrElse(false)

  def listFsMethods(): Unit = 
    val fsMethods = js.Object.keys((FS.asInstanceOf[js.Object]))
    println ("Methods in fs namespace: ")
    fsMethods.foreach{ m =>
      println(s"$m")
    }
end FileUtils