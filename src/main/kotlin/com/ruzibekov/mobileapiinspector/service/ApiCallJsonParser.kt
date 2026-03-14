package com.ruzibekov.mobileapiinspector.service

import com.ruzibekov.mobileapiinspector.model.ApiCallRecord
import com.ruzibekov.mobileapiinspector.model.ApiErrorPayload
import com.ruzibekov.mobileapiinspector.model.ApiMessage
import com.ruzibekov.mobileapiinspector.model.ApiResponsePayload
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object ApiCallJsonParser {
  private val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
  }

  fun parse(payload: String): ApiCallRecord {
    val root = json.parseToJsonElement(payload).jsonObject

    return ApiCallRecord(
      id = root.string("id") ?: error("id is required"),
      platform = root.string("platform") ?: "unknown",
      sourceId = root.string("sourceId") ?: root.string("platform") ?: "unknown",
      sourceName = root.string("sourceName") ?: root.string("platform") ?: "Unknown App",
      environment = root.string("environment"),
      timestamp = root.string("timestamp") ?: "",
      request = root.objectValue("request")?.toApiMessage()
        ?: error("request is required"),
      response = root.objectValue("response")?.toApiResponsePayload(),
      error = root.objectValue("error")?.toApiErrorPayload(),
      durationMs = root["durationMs"]?.jsonPrimitive?.intOrNull ?: 0
    )
  }

  fun formatJson(rawValue: String?): String {
    if (rawValue.isNullOrBlank()) {
      return ""
    }

    return runCatching {
      json.encodeToString(JsonElement.serializer(), json.parseToJsonElement(rawValue))
    }.getOrDefault(rawValue)
  }

  private fun JsonObject.toApiMessage(): ApiMessage {
    return ApiMessage(
      method = string("method") ?: "",
      url = string("url") ?: "",
      path = string("path") ?: "",
      headers = stringMap("headers"),
      queryParameters = stringMap("queryParameters"),
      body = string("body"),
      contentType = string("contentType")
    )
  }

  private fun JsonObject.toApiResponsePayload(): ApiResponsePayload {
    return ApiResponsePayload(
      statusCode = this["statusCode"]?.jsonPrimitive?.intOrNull,
      statusMessage = string("statusMessage"),
      headers = stringMap("headers"),
      body = string("body"),
      contentType = string("contentType")
    )
  }

  private fun JsonObject.toApiErrorPayload(): ApiErrorPayload {
    return ApiErrorPayload(
      type = string("type") ?: "unknown",
      message = string("message") ?: ""
    )
  }

  private fun JsonObject.stringMap(key: String): Map<String, String> {
    return objectValue(key)
      ?.entries
      ?.associate { it.key to (it.value.jsonPrimitive.contentOrNull ?: "") }
      ?: emptyMap()
  }

  private fun JsonObject.string(key: String): String? {
    return this[key]?.jsonPrimitive?.contentOrNull
  }

  private fun JsonObject.objectValue(key: String): JsonObject? {
    val value = this[key] ?: return null
    if (value is JsonNull) {
      return null
    }
    return value as? JsonObject
  }
}
