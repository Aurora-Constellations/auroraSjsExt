package com.axiom.audio

import com.axiom.recorder.AudioRecorder
import com.axiom.whispercpp.{WhisperCppConfig, WhisperCppRunner}
import better.files.File

object WhisperCppQuickTest:
  private val recordingDir = File("./recordings")
  private val testFile = recordingDir / "quick-test-whispercpp.wav"
  private val recordMillis = 5000

  def main(args: Array[String]): Unit =
    recordingDir.createDirectories()
    println(s"Recording for ${recordMillis / 1000} seconds. Speak now...")

    AudioRecorder.startRecording(testFile.pathAsString) match
      case Right(_) =>
        try Thread.sleep(recordMillis)
        finally AudioRecorder.stopRecording()

        WhisperCppConfig.load() match
          case Left(err) =>
            println(err)
          case Right(config) =>
            WhisperCppRunner.transcribe(testFile.pathAsString, config) match
              case Right(text) =>
                println("whisper.cpp transcription:")
                println(text)
              case Left(err) =>
                println(s"whisper.cpp transcription failed: $err")
      case Left(err) =>
        println(s"Unable to start recording: $err")
