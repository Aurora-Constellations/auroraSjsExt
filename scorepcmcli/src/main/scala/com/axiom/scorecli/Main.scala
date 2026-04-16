package com.axiom.scorecli

import scala.scalajs.js
import scala.scalajs.js.annotation.*
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

object Main:

  @js.native
  @JSImport("process", JSImport.Namespace)
  private object Process extends js.Object:
    val argv: js.Array[String] = js.native
    def exit(code: Int): Unit = js.native

  def main(args: Array[String]): Unit =
    val effectiveArgs =
      if args.nonEmpty then args.toList
      else Process.argv.toList.drop(2)

    effectiveArgs.headOption match
      case Some(path) =>
        ScorePcmCli.scoreFile(path).onComplete {
          case Success(report) =>
            println(ScorePcmCli.renderJson(report))
          case Failure(error) =>
            Console.err.println(error.getMessage)
            Process.exit(1)
        }
      case None =>
        Console.err.println("Usage: score-pcm <path-to-pcm.aurora>")
        Process.exit(1)
