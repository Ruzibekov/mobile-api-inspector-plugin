package com.ruzibekov.mobileapiinspector.service

import kotlin.test.Test
import kotlin.test.assertEquals

class ApiInspectorPortResolverTest {
  @Test
  fun `builds candidate ports from preferred port`() {
    val ports = ApiInspectorPortResolver.candidatePorts(63345)

    assertEquals(listOf(63345, 63346, 63347, 63348, 63349), ports.take(5))
    assertEquals(ApiInspectorConstants.portScanCount, ports.size)
  }

  @Test
  fun `stops at valid port range`() {
    val ports = ApiInspectorPortResolver.candidatePorts(65534)

    assertEquals(listOf(65534, 65535), ports)
  }
}
