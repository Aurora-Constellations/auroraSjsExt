package com.axiom.MergePCM

import typings.vscode.mod as vscode
import typings.auroraLangium.cliMod.parse
import vscode.ExtensionContext

import org.aurora.sjsast.*
// Use alias to distinguish between the two PCM types
import org.aurora.sjsast.{PCM => ProcessedPCM}
import org.aurora.sjsast.JoinMeet.*
import org.aurora.sjsast.JoinMeet.given
import org.aurora.sjsast.Show.*
import org.aurora.sjsast.Show.given
import org.aurora.sjsast.scoring.ScoreModuleResolver

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.annotation.*

object MergePCM:
  final case class MergeResult(
      content: String,
      warnings: List[String] = Nil
  )

  private final case class ModuleResolution(
      modulePaths: Map[String, String] = Map.empty,
      warnings: List[String] = Nil
  )

  private final case class LoadedModules(
      pcms: List[ProcessedPCM] = Nil,
      warnings: List[String] = Nil
  )

  @js.native
  @JSImport("fs", JSImport.Namespace)
  private object Fs extends js.Object:
    def existsSync(path: String): Boolean = js.native

  @js.native
  @JSImport("path", JSImport.Namespace)
  private object Path extends js.Object:
    def join(paths: String*): String = js.native

  def parseIssues(currentPCM: GenAst.PCM): Map[String, String] =
    // Extract imports from Issues section
    currentPCM.elements.flatMap { element =>
      if element.$type == "Issues" then
        val issues = element.asInstanceOf[GenAst.Issues]
        issues.coord.flatMap { coord =>
          val alias = coord.name
          // Get the module name from the first mod reference using $refText
          coord.mods.headOption match
            case Some(modRef) =>
              // Use $refText which contains the actual text reference
              val refText = modRef.asInstanceOf[js.Dynamic].selectDynamic("$refText")
              if refText != js.undefined then Some(refText.asInstanceOf[String] -> alias)
              else
                // println(s"No refText for $alias")
                None
            case None =>
              // println(s"No mods for coordinate $alias")
              None
        }.toSeq
      else Seq.empty[(String, String)]
    }.toMap

  def getModuleURIs(currentPCM: GenAst.PCM, moduleNames: Set[String]): Map[String, String] =
    currentPCM.$document.toOption match
      case Some(doc) =>
        val currentURI = doc.uri.toString
        val url = js.Dynamic.global.require("url")
        val path = js.Dynamic.global.require("path")

        // Use Node.js URL API to properly convert file:// URI to file path
        // This handles Windows paths correctly across all platforms
        val fileURLToPath = url.asInstanceOf[js.Dynamic].fileURLToPath
        val filePath = fileURLToPath(currentURI).asInstanceOf[String]

        val baseDir = path.dirname(filePath).asInstanceOf[String]

        moduleNames.map { moduleName =>
          val modulePath = path.join(baseDir, s"$moduleName.aurora").toString
          moduleName -> modulePath
        }.toMap
      case None =>
        Map.empty

  def parseModulesFromURIs(moduleURIs: Map[String, String], aliases: Map[String, String]): Future[List[ProcessedPCM]] =

    val pcmFutures = moduleURIs.map { case (moduleName, modulePath) =>

      parse(modulePath).toFuture.map { parsed =>
        try
          // Convert GenAst.PCM to ProcessedPCM
          val astPCM = parsed.asInstanceOf[GenAst.PCM]
          val module = Module(astPCM)
          val modulePCM = ModulePCM(module)
          val alias = aliases.getOrElse(moduleName, moduleName)
          val result = modulePCM.toPCM(alias)
          result
        catch
          case e: Exception =>
            println(s"Error converting $moduleName: ${e.getMessage}")
            e.printStackTrace()
            ProcessedPCM()
      }.recover {
        case e: Exception =>
          println(s"Parse error for $moduleName: ${e.getMessage}")
          e.printStackTrace()
          ProcessedPCM()
      }
    }.toList

    Future.sequence(pcmFutures)



  def generateOrdersDSL(currentPCM: GenAst.PCM): Future[String] =
    val moduleImports = parseIssues(currentPCM)
    val moduleNames = moduleImports.keySet
    val moduleURIs = getModuleURIs(currentPCM, moduleNames)

    // 1. Convert local file to ProcessedPCM (IR)
    val localPCM = ProcessedPCM(currentPCM)

    parseModulesFromURIs(moduleURIs, moduleImports).map { modulePCMs =>
      val localPCM = ProcessedPCM(currentPCM)

      // Merge only Clinical and Orders from modules, but keep local Issues
      val mergedPCM = (localPCM :: modulePCMs).reduce(_ |+| _)

      // If you want to strictly keep ONLY local issues:
      val finalPCM = mergedPCM.copy(
        cio = mergedPCM.cio.updated("Issues", localPCM.cio("Issues"))
      )

      val modeledPCM = ParametricModeling.applyAgeConstraint(finalPCM)
      modeledPCM.show
    }

  def generateOrdersDSLScoring(
      currentPCM: GenAst.PCM,
      scoreModuleResolverOptions: ScoreModuleResolver.Options = ScoreModuleResolver.Options.default
  ): Future[String] =
    generateOrdersDSLScoringResult(currentPCM, scoreModuleResolverOptions).map(_.content)

  def generateOrdersDSLScoringResult(
      currentPCM: GenAst.PCM,
      scoreModuleResolverOptions: ScoreModuleResolver.Options = ScoreModuleResolver.Options.default
  ): Future[MergeResult] =
    val issueImports = parseIssues(currentPCM)
    val issueResolution = resolveLocalModulePaths(currentPCM, issueImports.keySet)
    // 1. Convert local file to ProcessedPCM (IR)
    val localPCM = ProcessedPCM(currentPCM)

    loadModules(issueResolution.modulePaths, issueImports).flatMap { loadedIssueModules =>
      val mergedPCM = mergeAll(localPCM, loadedIssueModules.pcms)
      // Merge only Clinical and Orders from modules, but keep local Issues
      // If you want to strictly keep ONLY local issues:
      val finalPCM = withIssuesFrom(mergedPCM, localPCM)
      val modeledPCM = ParametricModeling.applyAgeConstraint(finalPCM)
      val scored = ClinicalScoring(modeledPCM)
      val scoreResolution = ScoreModuleResolver.resolve(
        currentPCM,
        scored.derivedModuleImports.keySet,
        scoreModuleResolverOptions
      )

      loadModules(scoreResolution.modulePaths, scored.derivedModuleImports).map { loadedScoreModules =>
        val mergedWithScores = mergeAll(scored.pcm, loadedScoreModules.pcms)
        val finalWithScores = withIssuesFrom(mergedWithScores, scored.pcm)
        MergeResult(
          content = finalWithScores.show,
          warnings =
            issueResolution.warnings ++
              loadedIssueModules.warnings ++
              scoreResolution.warnings ++
              loadedScoreModules.warnings
        )
      }
    }

  def generateOrdersDSLResult(
      currentPCM: GenAst.PCM,
      scoreModuleResolverOptions: ScoreModuleResolver.Options = ScoreModuleResolver.Options.default
  ): Future[MergeResult] =
    generateOrdersDSLScoringResult(currentPCM, scoreModuleResolverOptions)

  def extractSectionBeforeOrders(content: String): String =
    val lines = content.split("\n")
    val ordersIndex = lines.indexWhere(_.trim.startsWith("Orders:"))
    if ordersIndex >= 0 then lines.take(ordersIndex).mkString("\n").trim else content.trim

  def replaceFileContent(newContent: String): Unit =
    vscode.window.activeTextEditor.foreach { ed =>
      val lastLine = ed.document.lineCount - 1
      val lastChar = ed.document.lineAt(lastLine).range.end
      val fullRange = new vscode.Range(new vscode.Position(0, 0), lastChar)
      ed.edit(_.replace(fullRange, newContent))
    }

  def prettyPrint(pcm: ProcessedPCM): String =
    pcm.show

  private def resolveLocalModulePaths(currentPCM: GenAst.PCM, moduleNames: Iterable[String]): ModuleResolution =
    // Use Node.js URL API to properly convert file:// URI to file path
    // This handles Windows paths correctly across all platforms
    ScoreModuleResolver.currentPCMBaseDir(currentPCM) match
      case Some(baseDir) =>
        val entries = moduleNames.toList.distinct.sorted.map { moduleName =>
          val path = Path.join(baseDir, s"$moduleName.aurora")
          moduleName -> path
        }

        val modulePaths = entries.collect { case (moduleName, path) if Fs.existsSync(path) => moduleName -> path }.toMap
        val warnings = entries.collect {
          case (moduleName, path) if !Fs.existsSync(path) =>
            s"Unable to resolve local module '$moduleName' at '$path'."
        }

        ModuleResolution(modulePaths = modulePaths, warnings = warnings)

      case None if moduleNames.iterator.nonEmpty =>
        ModuleResolution(warnings = List("Unable to resolve local module paths for the current PCM."))

      case None =>
        ModuleResolution()

  private def loadModules(modulePaths: Map[String, String], aliases: Map[String, String]): Future[LoadedModules] =
    if modulePaths.isEmpty then Future.successful(LoadedModules())
    else
      val moduleFutures = modulePaths.toList.map { case (moduleName, modulePath) =>
        parse(modulePath).toFuture.map { parsed =>
          try
            // Convert GenAst.PCM to ProcessedPCM
            val astPCM = parsed.asInstanceOf[GenAst.PCM]
            val modulePCM = ModulePCM(Module(astPCM))
            Right(modulePCM.toPCM(aliases.getOrElse(moduleName, moduleName)))
          catch
            case e: Exception => Left(s"Error converting module '$moduleName': ${e.getMessage}")
        }.recover {
          case e: Exception => Left(s"Parse error for module '$moduleName': ${e.getMessage}")
        }
      }

      Future.sequence(moduleFutures).map { results =>
        LoadedModules(
          pcms = results.collect { case Right(pcm) => pcm },
          warnings = results.collect { case Left(warning) => warning }
        )
      }

  private def mergeAll(base: ProcessedPCM, others: List[ProcessedPCM]): ProcessedPCM =
    (base :: others).reduce(_ |+| _)

  private def withIssuesFrom(target: ProcessedPCM, source: ProcessedPCM): ProcessedPCM =
    val updatedCio = LHMap.from(target.cio)
    source.cio.get("Issues") match
      case Some(issues) => updatedCio.update("Issues", issues)
      case None         => updatedCio.remove("Issues")
    target.copy(cio = updatedCio)
