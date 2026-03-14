package com.ruzibekov.mobileapiinspector.ui

import com.ruzibekov.mobileapiinspector.model.ApiCallRecord

object ApiPinnedRecords {
  fun sort(
    records: List<ApiCallRecord>,
    pinnedIds: Set<String>
  ): List<ApiCallRecord> {
    return records.sortedWith(
      compareByDescending<ApiCallRecord> { pinnedIds.contains(it.id) }
    )
  }
}
