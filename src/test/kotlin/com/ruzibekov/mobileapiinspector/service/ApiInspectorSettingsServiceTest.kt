package com.ruzibekov.mobileapiinspector.service

import kotlin.test.Test
import kotlin.test.assertEquals

class ApiInspectorSettingsServiceTest {
  @Test
  fun `uses defaults for empty state`() {
    val service = ApiInspectorSettingsService()

    service.loadState(ApiInspectorSettingsState(host = "", port = 0))

    assertEquals(ApiInspectorConstants.defaultHost, service.host)
    assertEquals(ApiInspectorConstants.defaultPort, service.port)
  }

  @Test
  fun `updates host and port`() {
    val service = ApiInspectorSettingsService()

    service.update("0.0.0.0", 54321)

    assertEquals("0.0.0.0", service.host)
    assertEquals(54321, service.port)
  }
}
