package com.axiom.audio

import com.axiom.recorder.AudioRecorder
import com.axiom.whisper.WhisperTranscriber
import better.files.File

object QuickTest:
  private val recordingDir = File("./recordings")
  private val testFile = recordingDir / "quick-test.wav"
  private val recordMillis = 5000

  def main(args: Array[String]): Unit =
    recordingDir.createDirectories()
    println(s"Recording for ${recordMillis / 1000} seconds. Speak now...")

    AudioRecorder.startRecording(testFile.pathAsString) match
      case Right(_) =>
        try
          Thread.sleep(recordMillis)
        finally
          AudioRecorder.stopRecording()

        WhisperTranscriber.transcribeWavFile(testFile.pathAsString) match
          case Right(text) =>
            println("Transcription result:")
            println(text)
          case Left(err) =>
            println(s"Transcription failed: $err")
      case Left(err) =>
        println(s"Unable to start recording: $err")
