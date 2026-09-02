package org.aurora.sjsast.scoring

import org.aurora.sjsast.GenAst

import scala.scalajs.js
import scala.scalajs.js.annotation.*

object ScoreModuleResolver:
  final case class Options(bundledRoots: List[String])

  object Options:
    def default: Options =
      Options(defaultBundledRoots)

    def forExtensionRoot(extensionRoot: String): Options =
      Options(List(Path.resolve(extensionRoot, "pcmalgebra", "score-modules")))

  final case class Resolution(
      modulePaths: Map[String, String],
      warnings: List[String]
  )

  @js.native
  @JSImport("fs",JSImport.Namespace)
  private object Fs extends js.Object:
    def existsSync(path: String): Boolean = js.native

  @js.native
  @JSImport("path",JSImport.Namespace)
  private object Path extends js.Object:
    def dirname(path: String): String = js.native
    def join(paths: String*): String = js.native
    def resolve(paths: String*): String = js.native

  @js.native
  @JSImport("process",JSImport.Namespace)
  private object Process extends js.Object:
    val argv: js.Array[String] = js.native
    def cwd(): String = js.native

  @js.native
  @JSImport("url",JSImport.Namespace)
  private object Url extends js.Object:
    def fileURLToPath(url: String): String = js.native

  def currentPCMBaseDir(currentPCM: GenAst.PCM): Option[String] =
    currentPCM.$document.toOption.map { document =>
      Path.dirname(Url.fileURLToPath(document.uri.toString))
    }

  def resolve(
      currentPCM: GenAst.PCM,
      moduleNames: Iterable[String],
      options: Options = Options.default
  ): Resolution =
    val baseDir = currentPCMBaseDir(currentPCM)
    val entries = moduleNames.toList.distinct.sorted.map { moduleName =>
      moduleName -> resolveModulePath(baseDir, moduleName, options.bundledRoots)
    }

    val modulePaths = entries.collect { case (moduleName, Some(path)) => moduleName -> path }.toMap
    val warnings = entries.collect {
      case (moduleName, None) => s"Unable to resolve score module '$moduleName'."
    }

    Resolution(modulePaths = modulePaths, warnings = warnings)

  private def resolveModulePath(
      baseDir: Option[String],
      moduleName: String,
      bundledRoots: List[String]
  ): Option[String] =
    val localCandidates = baseDir.toList.map(root => Path.join(root, s"$moduleName.aurora"))
    val bundledCandidates = bundledRoots.map(root => Path.join(root, s"$moduleName.aurora"))
    (localCandidates ++ bundledCandidates).find(Fs.existsSync)

  private def defaultBundledRoots: List[String] =
    val cwd = Process.cwd()
    val entrypointRoots = Process.argv.toList.lift(1).toList.flatMap { entrypoint =>
      val entrypointDir = Path.dirname(Path.resolve(entrypoint))
      List(
        Path.resolve(entrypointDir, "..", "pcmalgebra", "score-modules"),
        Path.resolve(entrypointDir, "..", "score-modules")
      )
    }

    (
      List(
        Path.resolve(cwd, "pcmalgebra", "score-modules"),
        Path.resolve(cwd, "score-modules")
      ) ++ entrypointRoots
    ).distinct
