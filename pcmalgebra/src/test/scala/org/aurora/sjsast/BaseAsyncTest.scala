package org.aurora.sjsast

import org.scalatest._

import wordspec._
import matchers._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.scalajs.js
export org.aurora.utils.{fileutils, fs}

class BaseAsyncTest extends wordspec.AsyncWordSpec with should.Matchers {
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
  private lazy val basefilename = this.getClass.getSimpleName.replace("Test", "")
  private lazy val fullyQualifiedName = this.getClass.getName.replace("Test", "").replace(".", fileutils.separator)
  private lazy val testPath = s"$testResourcesPath${fileutils.separator}$fullyQualifiedName"

  // info() wrapped in a future
  protected def finfo(output: String) = Future(info(s"$output"))

  def testfilepath(name: String) =
    val path = s"$testPath-$name.aurora"
    if !fs.existsSync(path) then fail(s"missing test fixture: $path")
    path

  def testfilepath(index: Int): String = testfilepath(index.toString)

  def ftestfilepath(index: Int) = Future(testfilepath(index))

  def testfiletext(index: Int) =
    val path = testfilepath(index)
    fileutils.readFileSync(path)

  def ftestfiletext(index: Int) = Future(testfiletext(index))

  def parse(name: String): Future[GenAst.PCM] = fileutils
    .parse(testfilepath(name))
    .toFuture
    .recover(
      {
        case _: js.JavaScriptException =>
          // Handle JavaScript parsing errors
          fail("Parse failed with JavaScript error")
        case ex: Exception =>
          // Handle other exceptions
          fail(s"Parse failed: ${ex.getMessage}")
      }
    )

  def parse(index: Int): Future[GenAst.PCM] = parse(index.toString)

  def ir(name: String): Future[PCM] = parse(name).map(PCM(_))

  def ir(index: Int): Future[PCM] = ir(index.toString)

}
