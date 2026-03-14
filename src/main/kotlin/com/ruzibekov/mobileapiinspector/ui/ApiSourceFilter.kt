package com.ruzibekov.mobileapiinspector.ui

import com.ruzibekov.mobileapiinspector.model.ApiCallRecord

sealed interface ApiSourceFilter {
  fun matches(record: ApiCallRecord): Boolean

  data object All : ApiSourceFilter {
    override fun matches(record: ApiCallRecord): Boolean = true

    override fun toString(): String = "All apps"
  }

  data class Item(
    val sourceId: String,
    val sourceName: String
  ) : ApiSourceFilter {
    override fun matches(record: ApiCallRecord): Boolean {
      return record.sourceId == sourceId
    }

    override fun toString(): String = sourceName
  }
}
