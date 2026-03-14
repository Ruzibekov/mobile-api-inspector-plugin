package com.ruzibekov.mobileapiinspector.service

object ApiInspectorPortResolver {
  fun candidatePorts(preferredPort: Int): List<Int> {
    return buildList {
      for (offset in 0 until ApiInspectorConstants.portScanCount) {
        val port = preferredPort + offset
        if (port in 1..65535) {
          add(port)
        }
      }
    }
  }
}
