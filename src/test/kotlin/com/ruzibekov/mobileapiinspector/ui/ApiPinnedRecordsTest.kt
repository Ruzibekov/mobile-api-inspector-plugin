package com.ruzibekov.mobileapiinspector.ui

import com.ruzibekov.mobileapiinspector.model.ApiCallRecord
import com.ruzibekov.mobileapiinspector.model.ApiMessage
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiPinnedRecordsTest {
  @Test
  fun `moves pinned records to top while preserving input order`() {
    val first = createRecord("1")
    val second = createRecord("2")
    val third = createRecord("3")

    val sorted = ApiPinnedRecords.sort(
      records = listOf(first, second, third),
      pinnedIds = setOf("2")
    )

    assertEquals(listOf("2", "1", "3"), sorted.map { it.id })
  }

  private fun createRecord(id: String): ApiCallRecord {
    return ApiCallRecord(
      id = id,
      platform = "web",
      sourceId = "sample_web_app",
      sourceName = "Sample Web App",
      environment = "local",
      timestamp = "2026-03-14T12:00:00Z",
      request = ApiMessage(
        method = "GET",
        url = "https://api.example.com/items/$id",
        path = "/items/$id",
        headers = emptyMap(),
        queryParameters = emptyMap(),
        body = null,
        contentType = "application/json"
      ),
      response = null,
      error = null,
      durationMs = 100
    )
  }
}
