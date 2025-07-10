package com.axiom.messaging

import zio.json.*

// Message model
sealed trait Message

case class Request[T](command: String, data: T) extends Message
case class Response[R](command: String, result: R) extends Message

// Payload types
case class CreateAuroraFile(name: String) derives JsonEncoder, JsonDecoder
case class OpenAuroraFile(name: String) derives JsonEncoder, JsonDecoder
case class UpdateNarratives(source: String, unitNumber: String, flag: String) derives JsonEncoder, JsonDecoder
case class AddedToDB(name: String) derives JsonEncoder, JsonDecoder

// JSON codecs
// given JsonCodec[CreateAuroraFile] = DeriveJsonCodec.gen[CreateAuroraFile]
// given JsonCodec[OpenAuroraFile]   = DeriveJsonCodec.gen[OpenAuroraFile]
// given JsonCodec[UpdateNarratives] = DeriveJsonCodec.gen[UpdateNarratives]
// given JsonCodec[AddedToDB]        = DeriveJsonCodec.gen[AddedToDB]

// Workaround to decode raw JSON into typed request
case class RawRequest(command: String, data: String) derives JsonCodec
case class RawResponse(command: String, result: String) derives JsonCodec
