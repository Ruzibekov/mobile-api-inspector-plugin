package com.ruzibekov.mobileapiinspector.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiCallRecordTest {
  @Test
  fun `title uses host and path without scheme`() {
    val record = ApiCallRecord(
      id = "1",
      platform = "web",
      sourceId = "best-tracker-web",
      sourceName = "Best Tracker Web",
      environment = "local",
      timestamp = "2026-03-14T12:00:00Z",
      request = ApiMessage(
        method = "POST",
        url = "https://api.best-tracker.com/api/v1/devices/dee18240-08c0-4c83-a1ad-29a5e812ed0a/locations/?page=1",
        path = "/api/v1/devices/dee18240-08c0-4c83-a1ad-29a5e812ed0a/locations/",
        headers = emptyMap(),
        queryParameters = mapOf("page" to "1"),
        body = null,
        contentType = "application/json"
      ),
      response = null,
      error = null,
      durationMs = 42
    )

    assertEquals(
      "api.best-tracker.com/api/v1/devices/dee18240-08c0-4c83-a1ad-29a5e812ed0a/locations/",
      record.title
    )
    assertTrue(record.subtitle.contains("Best Tracker Web"))
    assertTrue(record.searchableText.contains("web"))
  }
}
