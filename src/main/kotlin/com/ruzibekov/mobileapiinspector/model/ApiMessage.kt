package com.ruzibekov.mobileapiinspector.model

data class ApiMessage(
  val method: String,
  val url: String,
  val path: String,
  val headers: Map<String, String>,
  val queryParameters: Map<String, String>,
  val body: String?,
  val contentType: String?
)
