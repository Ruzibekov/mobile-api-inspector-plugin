package com.ruzibekov.mobileapiinspector.model

import java.net.URI

data class ApiCallRecord(
  val id: String,
  val platform: String,
  val sourceId: String,
  val sourceName: String,
  val environment: String?,
  val timestamp: String,
  val request: ApiMessage,
  val response: ApiResponsePayload?,
  val error: ApiErrorPayload?,
  val durationMs: Int
) {
  val title: String
    get() = runCatching {
      val parsedUri = URI(request.url)
      val host = parsedUri.host.orEmpty()
      val path = parsedUri.rawPath.orEmpty()
      when {
        host.isNotBlank() && path.isNotBlank() -> "$host$path"
        host.isNotBlank() -> host
        request.path.isNotBlank() -> request.path
        else -> request.url
      }
    }.getOrElse {
      if (request.path.isNotBlank()) request.path else request.url
    }

  val subtitle: String
    get() = buildString {
      append(sourceName)
      append(" • ")
      append(platform.uppercase())
      if (!environment.isNullOrBlank()) {
        append(" • ")
        append(environment.uppercase())
      }
      if (response?.statusCode != null) {
        append(" • ")
        append(response.statusCode)
      } else if (error != null) {
        append(" • ERROR")
      }
      append(" • ")
      append(durationMs)
      append(" ms")
    }

  val searchableText: String
    get() = listOf(
      request.method,
      platform,
      sourceId,
      sourceName,
      environment.orEmpty(),
      request.url,
      request.path,
      response?.statusCode?.toString().orEmpty(),
      error?.message.orEmpty(),
      request.body.orEmpty(),
      response?.body.orEmpty()
    ).joinToString(" ").lowercase()
}
