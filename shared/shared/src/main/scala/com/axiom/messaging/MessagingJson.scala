package com.axiom.messaging

import zio.json.*

object MessagingJson {

  def encode[T: JsonEncoder](req: Request[T]): String =
    s"""{"command": "${req.command}", "data": ${req.data.toJson}}"""

  def encode[R: JsonEncoder](res: Response[R]): String =
    s"""{"command": "${res.command}", "result": ${res.result.toJson}}"""

  def decodeUpdateNarratives(raw: String): Option[Request[UpdateNarratives]] =
    raw.fromJson[RawRequest].toOption.flatMap { r =>
      r.data.fromJson[UpdateNarratives].toOption.map { parsed =>
        Request(r.command, parsed)
      }
    }

  def decodeCreateAuroraFile(raw: String): Option[Request[CreateAuroraFile]] =
    raw.fromJson[RawRequest].toOption.flatMap { r =>
      r.data.fromJson[CreateAuroraFile].toOption.map { parsed =>
        Request(r.command, parsed)
      }
    }

  def decodeOpenAuroraFile(raw: String): Option[Request[OpenAuroraFile]] =
    raw.fromJson[RawRequest].toOption.flatMap { r =>
      r.data.fromJson[OpenAuroraFile].toOption.map { parsed =>
        Request(r.command, parsed)
      }
    }

  def decodeAddedToDB(raw: String): Option[Response[AddedToDB]] =
    raw.fromJson[RawResponse].toOption.flatMap { r =>
      r.result.fromJson[AddedToDB].toOption.map { parsed =>
        Response(r.command, parsed)
      }
    }
}
