// package api

// import sttp.client3._
// import sttp.model._
// import io.circe.generic.auto._
// import io.circe.syntax._

// object ClaudeClient {

//   val apiKey = sys.env.getOrElse("CLAUDE_API_KEY", "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")

//   val backend = HttpURLConnectionBackend()
//   val apiUrl = uri"https://api.anthropic.com/v1/messages"

//   def askClaude(prompt: String): String = {
//     val headers = Map(
//       "x-api-key" -> apiKey,
//       "anthropic-version" -> "2023-06-01",
//       "Content-Type" -> "application/json"
//     )

//     val body = Map(
//       "model" -> "claude-3-opus-20240229",
//       "max_tokens" -> 1000,
//       "messages" -> List(Map("role" -> "user", "content" -> prompt))
//     ).asJson.noSpaces

//     val request = basicRequest
//       .headers(headers)
//       .body(body)
//       .post(apiUrl)

//     val response = request.send(backend)
//     response.body match {
//       case Right(result) => result
//       case Left(error)   => s"Error: $error"
//     }
//   }
// }
