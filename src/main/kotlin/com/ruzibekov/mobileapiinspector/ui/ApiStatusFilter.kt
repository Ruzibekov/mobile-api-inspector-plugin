package com.ruzibekov.mobileapiinspector.ui

enum class ApiStatusFilter(
  private val title: String
) {
  ALL("All status"),
  SUCCESS("2xx"),
  REDIRECTION("3xx"),
  CLIENT_ERROR("4xx"),
  SERVER_ERROR("5xx"),
  ERROR_ONLY("Errors");

  override fun toString(): String {
    return title
  }
}
