package com.axiom.MergePCM

import typings.vscode.mod as vscode
import typings.auroraLangium.cliMod.parse
import vscode.ExtensionContext

import org.aurora.sjsast.*
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
    currentPCM.elements.flatMap { element =>
      if element.$type == "Issues" then
        val issues = element.asInstanceOf[GenAst.Issues]
        issues.coord.flatMap { coord =>
          coord.mods.headOption.flatMap { modRef =>
            val refText = modRef.asInstanceOf[js.Dynamic].selectDynamic("$refText")
            if refText != js.undefined then Some(refText.asInstanceOf[String] -> coord.name)
            else None
          }
        }.toSeq
      else Seq.empty[(String, String)]
    }.toMap

  def generateOrdersDSL(currentPCM: GenAst.PCM): Future[String] =
    generateOrdersDSLResult(currentPCM).map(_.content)

  def generateOrdersDSLResult(
      currentPCM: GenAst.PCM,
      scoreModuleResolverOptions: ScoreModuleResolver.Options = ScoreModuleResolver.Options.default
  ): Future[MergeResult] =
    val issueImports = parseIssues(currentPCM)
    val issueResolution = resolveLocalModulePaths(currentPCM, issueImports.keySet)
    val localPCM = ProcessedPCM(currentPCM)

    loadModules(issueResolution.modulePaths, issueImports).flatMap { loadedIssueModules =>
      val mergedPCM = mergeAll(localPCM, loadedIssueModules.pcms)
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
