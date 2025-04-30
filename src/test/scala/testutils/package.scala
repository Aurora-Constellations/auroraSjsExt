package docere.testutils

export com.axiom.testutils.FileUtils.*
export Convertors.*
export scala.scalajs.js.JSConverters.*
export scala.concurrent.ExecutionContext.Implicits.global
export scala.concurrent.Future
export scala.util.Try

//the following two imports were helped by exporting them from arith-utils from the typesript project
export typings.auroraLangium.cliMod.AstUtils.{streamAllContents}
export typings.auroraLangium.cliMod.GenAst as GenAst
export typings.auroraLangium.cliMod.PCM
export typings.auroraLangium.cliMod.parse


val fExtension = "aurora"
val testFilesMath = List("math1", "math2", "math3").map{testFullPath}
val testFilesAurora = List("aurora1","aurora2").map{testFullPath}
val testIssuesAurora = List("issues1a","issues1b","issues1c").map{testIssuesPath}
val testClinicalsAurora = List("clinicals1","clinicals2","clinicals3").map{testClinicalsPath}
val testOrdersAurora = List("orders1","orders2","orders3").map{testOrdersPath}
val testQUAurora = List("qu1", "qu2", "qu3").map{testQUPath}

private  def testFullPath(name: String) = testAuroraFiles / s"$name.$fExtension"
private  def testIssuesPath(name:String) = testIssueFiles / s"$name.$fExtension"
private  def testClinicalsPath(name:String) = testClinicalsFiles / s"$name.$fExtension"
private  def testOrdersPath(name:String) = testOrdersFiles / s"$name.$fExtension"
private  def testQUPath(name:String) = testQUFiles / s"$name.$fExtension"