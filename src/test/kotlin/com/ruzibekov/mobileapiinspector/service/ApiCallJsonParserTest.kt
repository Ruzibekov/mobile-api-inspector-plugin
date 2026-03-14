package com.ruzibekov.mobileapiinspector.service

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiCallJsonParserTest {
  @Test
  fun `parses full payload`() {
    val record = ApiCallJsonParser.parse(
      """
      {
        "id": "1",
        "platform": "android",
        "sourceId": "sample_android_app",
        "sourceName": "Sample Android App",
        "environment": "debug",
        "timestamp": "2026-03-14T12:00:00Z",
        "durationMs": 120,
        "request": {
          "method": "GET",
          "url": "https://api.example.com/groups",
          "path": "/groups",
          "headers": { "Accept": "*/*" },
          "queryParameters": { "page": "1" },
          "body": "{\"ok\":true}",
          "contentType": "application/json"
        },
        "response": {
          "statusCode": 200,
          "statusMessage": "OK",
          "headers": { "Content-Type": "application/json" },
          "body": "{\"items\":[]}",
          "contentType": "application/json"
        }
      }
      """.trimIndent()
    )

    assertEquals("1", record.id)
    assertEquals("sample_android_app", record.sourceId)
    assertEquals("Sample Android App", record.sourceName)
    assertEquals(200, record.response?.statusCode)
    assertEquals("/groups", record.request.path)
  }

  @Test
  fun `falls back to raw text when value is not json`() {
    val formatted = ApiCallJsonParser.formatJson("plain text")

    assertEquals("plain text", formatted)
    assertTrue(formatted.isNotBlank())
  }

  @Test
  fun `accepts null response and error fields`() {
    val record = ApiCallJsonParser.parse(
      """
      {
        "id": "2",
        "platform": "android",
        "sourceId": "sample_android_app",
        "sourceName": "Sample Android App",
        "environment": "debug",
        "timestamp": "2026-03-14T12:05:00Z",
        "durationMs": 32,
        "request": {
          "method": "POST",
          "url": "https://api.example.com/auth/refresh",
          "path": "/auth/refresh",
          "headers": {},
          "queryParameters": {},
          "body": "{\"refresh_token\":\"***\"}",
          "contentType": "application/json"
        },
        "response": null,
        "error": null
      }
      """.trimIndent()
    )

    assertEquals("2", record.id)
    assertEquals(null, record.response)
    assertEquals(null, record.error)
    assertEquals("/auth/refresh", record.request.path)
  }
}
