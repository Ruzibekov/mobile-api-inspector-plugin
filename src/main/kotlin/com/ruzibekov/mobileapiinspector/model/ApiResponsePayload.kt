package com.ruzibekov.mobileapiinspector.model

data class ApiResponsePayload(
  val statusCode: Int?,
  val statusMessage: String?,
  val headers: Map<String, String>,
  val body: String?,
  val contentType: String?
)
