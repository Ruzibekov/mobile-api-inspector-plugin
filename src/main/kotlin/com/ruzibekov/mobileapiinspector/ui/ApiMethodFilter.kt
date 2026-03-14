package com.ruzibekov.mobileapiinspector.ui

enum class ApiMethodFilter(
  private val title: String
) {
  ALL("All methods"),
  GET("GET"),
  POST("POST"),
  PUT("PUT"),
  PATCH("PATCH"),
  DELETE("DELETE"),
  OTHER("Other");

  override fun toString(): String {
    return title
  }

  fun matches(method: String): Boolean {
    val normalizedMethod = method.uppercase()
    return when (this) {
      ALL -> true
      GET -> normalizedMethod == "GET"
      POST -> normalizedMethod == "POST"
      PUT -> normalizedMethod == "PUT"
      PATCH -> normalizedMethod == "PATCH"
      DELETE -> normalizedMethod == "DELETE"
      OTHER -> normalizedMethod !in standardMethods
    }
  }

  companion object {
    private val standardMethods = setOf("GET", "POST", "PUT", "PATCH", "DELETE")
  }
}
