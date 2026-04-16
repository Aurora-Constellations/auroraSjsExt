package com.axiom.scorecli

import org.aurora.sjsast.{ClinicalScoringConsoleSummary, PCM, GenAst}
import typings.auroraLangium.distTypesSrcExtensionSrcParserParserMod.parseFromText
import typings.auroraLangium.distTypesSrcLanguageAuroraModuleMod.createAuroraServices
import typings.langium.libLspDefaultLspModuleMod.DefaultSharedModuleContext
import ujson.*

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.annotation.*

object ScorePcmCli:

  final case class ScoredFileReport(
      file: String,
      scores: ClinicalScoringConsoleSummary.ScoreSummary
  )

  @js.native
  @JSImport("fs", JSImport.Namespace)
  private object Fs extends js.Object:
    def existsSync(path: String): Boolean = js.native
    def readFileSync(path: String, encoding: String): String = js.native

  @js.native
  @JSImport("path", JSImport.Namespace)
  private object Path extends js.Object:
    def basename(path: String): String = js.native
    def resolve(paths: String*): String = js.native

  def scoreFile(path: String): Future[ScoredFileReport] =
    if !Fs.existsSync(path) then
      Future.failed(new IllegalArgumentException(s"File does not exist: $path"))
    else
      val absolutePath = Path.resolve(path)
      val contents = Fs.readFileSync(absolutePath, "utf8")
      val fileSystemContext = js.Dynamic.literal(
        fileSystemProvider = (() => js.Dynamic.literal())
      ).asInstanceOf[DefaultSharedModuleContext]
      val aurora = createAuroraServices(fileSystemContext).Aurora
      parseFromText(aurora, contents).toFuture.map { parsed =>
        val pcm = PCM(parsed.asInstanceOf[GenAst.PCM])
        ScoredFileReport(
          file = Path.basename(absolutePath),
          scores = ClinicalScoringConsoleSummary.fromPcm(pcm)
        )
      }

  def renderJson(report: ScoredFileReport): String =
    Obj(
      "file" -> Str(report.file),
      "scores" -> Obj(
        "GCS_Adult" -> gcsJson(report.scores.GCS_Adult),
        "CHA2DS2_VASc" -> cha2ds2Json(report.scores.CHA2DS2_VASc)
      )
    ).render(indent = 2)

  private def gcsJson(summary: ClinicalScoringConsoleSummary.GcsAdultSummary): Obj =
    Obj.from(
      List(
        Some("status" -> Str(summary.status)),
        summary.gcs_total.map(value => "gcs_total" -> Num(value)),
        summary.gcs_total_source.map(value => "gcs_total_source" -> Str(value)),
        summary.gcs_severity.map(value => "gcs_severity" -> Str(value))
      ).flatten
    )

  private def cha2ds2Json(summary: ClinicalScoringConsoleSummary.Cha2Ds2VascSummary): Obj =
    Obj.from(
      List(
        Some("status" -> Str(summary.status)),
        summary.cha2ds2_vasc_total.map(value => "cha2ds2_vasc_total" -> Num(value)),
        summary.cha2ds2_vasc_risk_band.map(value => "cha2ds2_vasc_risk_band" -> Str(value))
      ).flatten
    )
