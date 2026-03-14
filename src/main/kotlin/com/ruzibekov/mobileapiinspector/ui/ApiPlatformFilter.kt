package com.ruzibekov.mobileapiinspector.ui

enum class ApiPlatformFilter(
  private val title: String
) {
  ALL("All apps"),
  WEB("Web"),
  IOS("iOS"),
  ANDROID("Android");

  override fun toString(): String {
    return title
  }

  fun matches(platform: String): Boolean {
    val normalizedPlatform = platform.lowercase()
    return when (this) {
      ALL -> true
      WEB -> normalizedPlatform == "web"
      IOS -> normalizedPlatform == "ios"
      ANDROID -> normalizedPlatform == "android"
    }
  }
}
