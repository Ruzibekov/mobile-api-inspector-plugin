package com.ruzibekov.mobileapiinspector.service

import com.ruzibekov.mobileapiinspector.model.ApiCallRecord
import com.ruzibekov.mobileapiinspector.model.ApiMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApiCallStoreServiceTest {
  @Test
  fun `does not store records while paused`() {
    val storeService = ApiCallStoreService()

    storeService.setPaused(true)
    storeService.addRecord(createRecord("1"))

    assertTrue(storeService.snapshot().isEmpty())
  }

  @Test
  fun `toggles pinned ids`() {
    val storeService = ApiCallStoreService()

    storeService.togglePinned("record-1")
    assertTrue(storeService.isPinned("record-1"))

    storeService.togglePinned("record-1")
    assertFalse(storeService.isPinned("record-1"))
  }

  @Test
  fun `stores records when capture is active`() {
    val storeService = ApiCallStoreService()

    storeService.addRecord(createRecord("1"))
    storeService.addRecord(createRecord("2"))

    assertEquals(listOf("2", "1"), storeService.snapshot().map { it.id })
  }

  private fun createRecord(id: String): ApiCallRecord {
    return ApiCallRecord(
      id = id,
      platform = "android",
      sourceId = "sample_android_app",
      sourceName = "Sample Android App",
      environment = "debug",
      timestamp = "2026-03-14T12:00:00Z",
      request = ApiMessage(
        method = "GET",
        url = "https://api.example.com/groups",
        path = "/groups",
        headers = emptyMap(),
        queryParameters = emptyMap(),
        body = null,
        contentType = "application/json"
      ),
      response = null,
      error = null,
      durationMs = 120
    )
  }
}
