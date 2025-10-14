package com.axiom.whisper

import sttp.client4.*
import sttp.client4.circe.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.parser.*
import better.files.{File => BFile, *}
import scala.util.{Try, Success, Failure, Using}
import scala.concurrent.duration.*
import java.io.FileInputStream
import java.util.Properties
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import better.files._
import cats.effect.IO
import cats.effect.unsafe.implicits.global

// Case classes for API request/response
case class WhisperResponse(text: String)
case class WhisperError(error: WhisperErrorDetails)
case class WhisperErrorDetails(`type`: String, message: String)

// Configuration case class
case class WhisperConfig(
  openAiApiKey: String,
  whisperModel: String = "whisper-1",
  defaultTemperature: Double = 0.0,
  defaultResponseFormat: String = "json"
)

object WhisperConfig :

  val inputStream = new FileInputStream(File(".env").toJava)// needs absolute path
  val props = new Properties()
  props.load(inputStream)
  val config = ConfigFactory.parseProperties(props)
  val whisperApiKey =  config.getString("WHISPER_API_KEY")
  def apply(): WhisperConfig = 
    WhisperConfig(
      openAiApiKey = whisperApiKey,"whisper-1",0.0,"json"
    )
  

object WhisperTranscriber :
  
  // JSON codecs
  implicit val whisperResponseDecoder: Decoder[WhisperResponse] = deriveDecoder
  implicit val whisperErrorDetailsDecoder: Decoder[WhisperErrorDetails] = deriveDecoder  
  implicit val whisperErrorDecoder: Decoder[WhisperError] = deriveDecoder
  
  // Configuration
  private val openAIBaseUrl = "https://api.openai.com/v1"
  private val whisperEndpoint = s"$openAIBaseUrl/audio/transcriptions"

  val whisperConfig = WhisperConfig()
  
  // HTTP client backend
  private val backend = DefaultSyncBackend()
  
  def transcribeAudioFile(audioFilePath: String): Either[String, String] = {
    val audioFile = File(audioFilePath)
    
    if (!audioFile.exists) {
      return Left(s"Audio file not found: $audioFilePath")
    }
    
    try {
      val request = basicRequest
        .post(uri"$whisperEndpoint")
        .header("Authorization", s"Bearer ${whisperConfig.openAiApiKey}")
        .multipartBody(
          multipart("file", audioFile.byteArray)
            .fileName(audioFile.name),
          multipart("model", whisperConfig.whisperModel),
          multipart("response_format", whisperConfig.defaultResponseFormat),
          multipart("temperature", whisperConfig.defaultTemperature.toString)
        )
        .response(asJson[WhisperResponse])
        
      val response = request.send(backend)
      
      response.body match {
        case Right(whisperResp) => 
          Right(whisperResp.text)
        case Left(errorBody) =>
          // Try to parse as error response
          val errorText = errorBody match {
            case HttpError(body, _) => body
            case other => other.toString
          }
          decode[WhisperError](errorText) match {
            case Right(whisperErr) => 
              Left(s"Whisper API Error: ${whisperErr.error.message}")
            case Left(_) => 
              Left(s"HTTP Error: $errorText")
          }
      }
    } catch {
      case ex: Exception =>
        Left(s"Exception during transcription: ${ex.getMessage}")
    }
  }
  
  def transcribeWavFile(wavFilePath: String): Either[String,String] = {
    println(s"Starting transcription of: $wavFilePath")
    
    val textResult = transcribeAudioFile(wavFilePath)
    textResult match {
      case Right(transcription) =>
        // println("Transcription completed successfully!")
        println("=" * 50)
        println(transcription)
        println("=" * 50)
        
        // Optionally save to a text file
        val outputFile = File(wavFilePath).parent / (File(wavFilePath).nameWithoutExtension + "_transcription.txt")
        outputFile.write(transcription)
        println(s"Transcription saved to: ${outputFile.pathAsString}")
        textResult
        
      case Left(error) =>
        println(s"Transcription failed: $error")
        textResult
    }
  }
  
  def main(args: Array[String]): Unit =   
    val wavFilePath = "./recordings/latest.wav"
    WhisperTranscriber.transcribeWavFile(wavFilePath)