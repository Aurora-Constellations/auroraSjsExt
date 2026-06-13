package org.aurora.sjsast


import org.scalatest._

import wordspec._
import matchers._
import scala.concurrent.Future
import org.aurora.utils.fileutils.createFileIfNotExists
import scala.concurrent.ExecutionContext
import scala.scalajs.js
export org.aurora.utils.{fileutils,fs}

class BaseAsyncTest extends wordspec.AsyncWordSpec with should.Matchers{
  export JoinMeet.given
  // export cats.syntax.semigroup._ // for |+|

  export scala.concurrent.Future
  // export cats.Show
  // export cats.syntax.show._ 
  // export Show.given
  export scala.scalajs.js.JSConverters._ 

  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
  override implicit def executionContext: ExecutionContext = queue

  private lazy val testResourcesPath = fileutils.testResourcesPath
  private lazy val basefilename = this.getClass.getSimpleName.replace("Test","")
  private lazy val fullyQualifiedName = this.getClass.getName.replace("Test","").replace(".",fileutils.separator)
  private lazy val testPath = s"$testResourcesPath${fileutils.separator}$fullyQualifiedName"




  //info() wrapped in a future
  protected def finfo(output:String) =  Future(output.linesIterator.foreach(info(_))   )
  protected def fnote(output:String) =  Future(output.linesIterator.foreach(note(_))   )
  protected def falert(output:String) =  Future(output.linesIterator.foreach(alert(_))   )

  //for file outputs of tests
  protected def tempoutputpath(name:String) = 
    val path = fileutils.cwd + fileutils.separator + "pcmalgebra" + fileutils.separator + "temp" + fileutils.separator + name
    createFileIfNotExists(path)
    path

  protected def testfilepath(index:Int) = 
    val path =  s"$testResourcesPath${fileutils.separator}$fullyQualifiedName-$index.aurora"
    createFileIfNotExists(path)
    path


  ////////
  // fileutils.writeFileSync(tempoutputpath("temp.txt"), text)  


  private def testfiletext(index:Int) = 
    val path = testfilepath(index)
    createFileIfNotExists(path)
    fileutils.readFileSync(path)

  protected def parse(index:Int) = fileutils.parse(testfilepath(index)).toFuture.recover(
      {
        case _: js.JavaScriptException => 
          // Handle JavaScript parsing errors
          fail("Parse failed with JavaScript error")
        case ex: Exception => 
          // Handle other exceptions
          fail(s"Parse failed: ${ex.getMessage}")
      }
    )

  def optParse(index:Int) = 
    fileutils.parse(testfilepath(index)).toFuture.recover(
      {
        case _: js.JavaScriptException => 
          // Handle JavaScript parsing errors
          println("Parse failed with JavaScript error")
          None
        case ex: Exception => 
          // Handle other exceptions
          println(s"Parse failed: ${ex.getMessage}")
          None
        case _ => println("Parse failed with unknown error")  
          None
      }
    ).map{ result =>
      if(result.isInstanceOf[Option[_]]) None
       else Some(result.asInstanceOf[typings.auroraLangium.distTypesSrcLanguageGeneratedAstMod.PCM])
    }

    


  def optIr(index:Int) = optParse(index).map(
    {
      case Some(langiumPCM) => Some(PCM(langiumPCM))
      case None => None
    }
  )

  import org.aurora.sjsast.Show.given
  import org.aurora.sjsast.Show.*
  def fshow(pcm:PCM) = Future(Show.show(pcm))

}