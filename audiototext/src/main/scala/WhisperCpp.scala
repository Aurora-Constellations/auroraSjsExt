package com.axiom.whispercpp

import better.files.File
import scala.sys.process.*
import scala.util.control.NonFatal

case class WhisperCppConfig(
  binaryPath: String,
  modelPath: String,
  extraArgs: Seq[String] = Seq("--no-timestamps")
)

object WhisperCppConfig:
  private val defaultBinaryCandidates = Seq(
    "./native/whispercpp/macos-arm64/whisper-cli",
    "./native/whispercpp/whisper-cli",
    "./whisper.cpp/whisper-cli",
    "./whisper.cpp/build/bin/whisper-cli",
    "./whisper.cpp/build/bin/main",
    "./whisper.cpp/main"
  )
  private val defaultModelCandidates = Seq(
    "./native/whispercpp/models/ggml-base.en.bin",
    "./resources/whisper/models/ggml-base.en.bin",
    "./whisper.cpp/models/ggml-base.en.bin"
  )

  def load(): Either[String, WhisperCppConfig] =
    val binaryFromEnv = sys.env.get("WHISPER_CPP_BINARY").map(File(_))
    val binary =
      binaryFromEnv
        .filter(_.exists)
        .orElse(defaultBinaryCandidates.map(File(_)).find(_.exists))
        .toRight(
          s"""whisper.cpp binary not found. Set WHISPER_CPP_BINARY or place one of:
             |${defaultBinaryCandidates.mkString("\n")}
             |""".stripMargin.trim
        )

    val modelFromEnv = sys.env.get("WHISPER_CPP_MODEL").map(File(_))
    val modelFile =
      modelFromEnv.filter(_.exists)
        .orElse(defaultModelCandidates.map(File(_)).find(_.exists))
        .toRight(
          s"""whisper.cpp model not found. Set WHISPER_CPP_MODEL or place a ggml model at one of:
             |${defaultModelCandidates.mkString("\n")}
             |""".stripMargin.trim
        )

    for
      binaryFile <- binary
      modelPath <- modelFile
    yield
      val resolvedBinary =
        if (binaryFile.name == "main") then
          val alt = binaryFile.parentOption.flatMap { parent =>
            val candidate = parent / "whisper-cli"
            if candidate.exists then Some(candidate) else None
          }
          alt.getOrElse(binaryFile)
        else binaryFile
      WhisperCppConfig(resolvedBinary.pathAsString, modelPath.pathAsString)

object WhisperCppRunner:
  def transcribe(wavPath: String, config: WhisperCppConfig): Either[String, String] =
    val audioFile = File(wavPath)
    if (!audioFile.exists) then
      return Left(s"Audio file not found: ${audioFile.pathAsString}")

    val tempDir = File.newTemporaryDirectory("whispercpp-")
    try
      val outputBase = (tempDir / "transcript").pathAsString
      val command =
        Seq(config.binaryPath, "-m", config.modelPath, "-f", audioFile.pathAsString, "-otxt", "-of", outputBase) ++ config.extraArgs

      val stdout = new StringBuilder
      val stderr = new StringBuilder
      val exitCode = Process(command).!(ProcessLogger(
        (out: String) => stdout.append(out).append(System.lineSeparator()),
        (err: String) => stderr.append(err).append(System.lineSeparator())
      ))

      if (exitCode != 0) then
        val errOut = stderr.result().trim
        val stdOut = stdout.result().trim
        val combined =
          (errOut, stdOut) match
            case (e, s) if e.nonEmpty && s.nonEmpty => s"$e\n$s"
            case (e, _) if e.nonEmpty => e
            case (_, s) if s.nonEmpty => s
            case _ => "no output captured"
        Left(s"whisper.cpp exited with code $exitCode: $combined")
      else
        val transcriptFile = File(outputBase + ".txt")
        if (transcriptFile.exists) then
          Right(transcriptFile.contentAsString.trim)
        else
          val consoleOut = stdout.result().trim
          if (consoleOut.nonEmpty) then Right(consoleOut)
          else Left("whisper.cpp finished but no transcript file was produced.")
    catch
      case NonFatal(ex) =>
        Left(s"Failed to invoke whisper.cpp: ${ex.getMessage}")
    finally
      tempDir.delete(swallowIOExceptions = true)
