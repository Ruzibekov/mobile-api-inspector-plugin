package com.ruzibekov.mobileapiinspector.ui

import com.ruzibekov.mobileapiinspector.model.ApiCallRecord
import com.ruzibekov.mobileapiinspector.model.ApiErrorPayload
import com.ruzibekov.mobileapiinspector.model.ApiMessage
import com.ruzibekov.mobileapiinspector.model.ApiResponsePayload
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApiRecordFilterTest {
  @Test
  fun `matches query against request url and body`() {
    val record = createRecord(
      method = "GET",
      path = "/watchers",
      body = """{"scope":"family"}""",
      statusCode = 200
    )

    assertTrue(
      ApiRecordFilter.matches(
        record = record,
        query = "watchers",
        sourceFilter = ApiSourceFilter.All,
        platformFilter = ApiPlatformFilter.ALL,
        methodFilter = ApiMethodFilter.ALL,
        statusFilter = ApiStatusFilter.ALL
      )
    )
    assertTrue(
      ApiRecordFilter.matches(
        record = record,
        query = "family",
        sourceFilter = ApiSourceFilter.All,
        platformFilter = ApiPlatformFilter.ALL,
        methodFilter = ApiMethodFilter.ALL,
        statusFilter = ApiStatusFilter.ALL
      )
    )
  }

  @Test
  fun `matches selected method only`() {
    val record = createRecord(
      method = "POST",
      path = "/auth/refresh",
      body = null,
      statusCode = 200
    )

    assertTrue(
      ApiRecordFilter.matches(
        record = record,
        query = "",
        sourceFilter = ApiSourceFilter.All,
        platformFilter = ApiPlatformFilter.ALL,
        methodFilter = ApiMethodFilter.POST,
        statusFilter = ApiStatusFilter.ALL
      )
    )
    assertFalse(
      ApiRecordFilter.matches(
        record = record,
        query = "",
        sourceFilter = ApiSourceFilter.All,
        platformFilter = ApiPlatformFilter.ALL,
        methodFilter = ApiMethodFilter.GET,
        statusFilter = ApiStatusFilter.ALL
      )
    )
  }

  @Test
  fun `matches status groups and errors`() {
    val successRecord = createRecord(
      method = "GET",
      path = "/groups",
      body = null,
      statusCode = 204
    )
    val serverErrorRecord = createRecord(
      method = "GET",
      path = "/groups",
      body = null,
      statusCode = 503
    )
    val failedRecord = createRecord(
      method = "GET",
      path = "/groups",
      body = null,
      statusCode = null,
      error = ApiErrorPayload(type = "connection", message = "failed")
    )

    assertTrue(
      ApiRecordFilter.matches(
        record = successRecord,
        query = "",
        sourceFilter = ApiSourceFilter.All,
        platformFilter = ApiPlatformFilter.ALL,
        methodFilter = ApiMethodFilter.ALL,
        statusFilter = ApiStatusFilter.SUCCESS
      )
    )
    assertTrue(
      ApiRecordFilter.matches(
        record = serverErrorRecord,
        query = "",
        sourceFilter = ApiSourceFilter.All,
        platformFilter = ApiPlatformFilter.ALL,
        methodFilter = ApiMethodFilter.ALL,
        statusFilter = ApiStatusFilter.SERVER_ERROR
      )
    )
    assertTrue(
      ApiRecordFilter.matches(
        record = failedRecord,
        query = "",
        sourceFilter = ApiSourceFilter.All,
        platformFilter = ApiPlatformFilter.ALL,
        methodFilter = ApiMethodFilter.ALL,
        statusFilter = ApiStatusFilter.ERROR_ONLY
      )
    )
    assertFalse(
      ApiRecordFilter.matches(
        record = successRecord,
        query = "",
        sourceFilter = ApiSourceFilter.All,
        platformFilter = ApiPlatformFilter.ALL,
        methodFilter = ApiMethodFilter.ALL,
        statusFilter = ApiStatusFilter.ERROR_ONLY
      )
    )
  }

  @Test
  fun `matches selected platform only`() {
    val webRecord = createRecord(
      method = "GET",
      path = "/watchers",
      body = null,
      statusCode = 200,
      platform = "web"
    )
    val iosRecord = createRecord(
      method = "GET",
      path = "/watchers",
      body = null,
      statusCode = 200,
      platform = "ios"
    )

    assertTrue(
      ApiRecordFilter.matches(
        record = webRecord,
        query = "",
        sourceFilter = ApiSourceFilter.All,
        platformFilter = ApiPlatformFilter.WEB,
        methodFilter = ApiMethodFilter.ALL,
        statusFilter = ApiStatusFilter.ALL
      )
    )
    assertFalse(
      ApiRecordFilter.matches(
        record = iosRecord,
        query = "",
        sourceFilter = ApiSourceFilter.All,
        platformFilter = ApiPlatformFilter.WEB,
        methodFilter = ApiMethodFilter.ALL,
        statusFilter = ApiStatusFilter.ALL
      )
    )
  }

  @Test
  fun `matches selected source only`() {
    val androidRecord = createRecord(
      method = "GET",
      path = "/watchers",
      body = null,
      statusCode = 200,
      platform = "android",
      sourceId = "sample_android_app",
      sourceName = "Sample Android App"
    )
    val webRecord = createRecord(
      method = "GET",
      path = "/watchers",
      body = null,
      statusCode = 200,
      platform = "web",
      sourceId = "sample_web_app",
      sourceName = "Sample Web App"
    )

    assertTrue(
      ApiRecordFilter.matches(
        record = androidRecord,
        query = "",
        sourceFilter = ApiSourceFilter.Item(
          sourceId = "sample_android_app",
          sourceName = "Sample Android App"
        ),
        platformFilter = ApiPlatformFilter.ALL,
        methodFilter = ApiMethodFilter.ALL,
        statusFilter = ApiStatusFilter.ALL
      )
    )
    assertFalse(
      ApiRecordFilter.matches(
        record = webRecord,
        query = "",
        sourceFilter = ApiSourceFilter.Item(
          sourceId = "sample_android_app",
          sourceName = "Sample Android App"
        ),
        platformFilter = ApiPlatformFilter.ALL,
        methodFilter = ApiMethodFilter.ALL,
        statusFilter = ApiStatusFilter.ALL
      )
    )
  }

  private fun createRecord(
    method: String,
    path: String,
    body: String?,
    statusCode: Int?,
    platform: String = "android",
    sourceId: String = "sample_android_app",
    sourceName: String = "Sample Android App",
    error: ApiErrorPayload? = null
  ): ApiCallRecord {
    return ApiCallRecord(
      id = "1",
      platform = platform,
      sourceId = sourceId,
      sourceName = sourceName,
      environment = "debug",
      timestamp = "2026-03-14T12:00:00Z",
      request = ApiMessage(
        method = method,
        url = "https://api.example.com$path",
        path = path,
        headers = emptyMap(),
        queryParameters = emptyMap(),
        body = body,
        contentType = "application/json"
      ),
      response = statusCode?.let {
        ApiResponsePayload(
          statusCode = it,
          statusMessage = "OK",
          headers = emptyMap(),
          body = body,
          contentType = "application/json"
        )
      },
      error = error,
      durationMs = 120
    )
  }
}
