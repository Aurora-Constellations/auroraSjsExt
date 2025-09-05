package com.axiom.audio

import com.axiom.recorder.AudioRecorder
import com.axiom.whisper.WhisperTranscriber
import scala.io.StdIn

object Main:
  def main(args: Array[String]): Unit =
    println("Audio backend started. Waiting for commands...")

    var continue = true
    while continue do
        val line = StdIn.readLine()
        if line == null then
            continue = false
        else
            val tokens = line.split(" ").toList
            tokens match
            case "record" :: filename :: _ =>
                AudioRecorder.startRecording(filename) match
                case Right(_)  => println(s"Recording started: $filename")
                case Left(err) => println(s"Error starting recording: $err")
            case "stop" :: _ =>
                AudioRecorder.stopRecording()
                println("Recording stopped.")
            case "transcribe" :: filepath :: _ =>
                WhisperTranscriber.transcribeWavFile(filepath)
            case "exit" :: _ =>
                println("Shutting down backend...")
                AudioRecorder.cleanup()
                continue = false
            case unknown =>
                println(s"Unknown command: ${unknown.mkString(" ")}")
