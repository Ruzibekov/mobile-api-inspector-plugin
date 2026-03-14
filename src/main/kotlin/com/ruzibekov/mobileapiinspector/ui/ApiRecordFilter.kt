package com.ruzibekov.mobileapiinspector.ui

import com.ruzibekov.mobileapiinspector.model.ApiCallRecord

object ApiRecordFilter {
  fun matches(
    record: ApiCallRecord,
    query: String,
    sourceFilter: ApiSourceFilter,
    platformFilter: ApiPlatformFilter,
    methodFilter: ApiMethodFilter,
    statusFilter: ApiStatusFilter
  ): Boolean {
    return matchesQuery(record, query) &&
      sourceFilter.matches(record) &&
      platformFilter.matches(record.platform) &&
      methodFilter.matches(record.request.method) &&
      matchesStatus(record, statusFilter)
  }

  private fun matchesQuery(
    record: ApiCallRecord,
    query: String
  ): Boolean {
    return query.isBlank() || record.searchableText.contains(query.lowercase())
  }

  private fun matchesStatus(
    record: ApiCallRecord,
    statusFilter: ApiStatusFilter
  ): Boolean {
    val statusCode = record.response?.statusCode
    return when (statusFilter) {
      ApiStatusFilter.ALL -> true
      ApiStatusFilter.SUCCESS -> statusCode in 200..299
      ApiStatusFilter.REDIRECTION -> statusCode in 300..399
      ApiStatusFilter.CLIENT_ERROR -> statusCode in 400..499
      ApiStatusFilter.SERVER_ERROR -> statusCode in 500..599
      ApiStatusFilter.ERROR_ONLY -> record.error != null || statusCode == null
    }
  }
}
