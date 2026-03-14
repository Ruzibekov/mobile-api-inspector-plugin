package com.ruzibekov.mobileapiinspector.ui

import com.intellij.openapi.Disposable

interface InspectorDetailViewer : Disposable {
  fun setContent(
    value: String,
    useJsonHighlighting: Boolean
  )

  fun highlight(query: String): Int
}
