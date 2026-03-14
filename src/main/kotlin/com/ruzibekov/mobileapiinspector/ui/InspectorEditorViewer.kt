package com.ruzibekov.mobileapiinspector.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JPanel

class InspectorEditorViewer(
  private val project: Project
) : JPanel(BorderLayout()), InspectorDetailViewer {
  private var editor: EditorEx? = null
  private var content: String = ""

  init {
    border = JBUI.Borders.empty()
    setContent("", false)
  }

  override fun setContent(
    value: String,
    useJsonHighlighting: Boolean
  ) {
    content = value
    replaceEditor(value, useJsonHighlighting)
  }

  override fun highlight(query: String): Int {
    val currentEditor = editor ?: return 0
    currentEditor.selectionModel.removeSelection()

    val normalizedQuery = query.trim()
    if (normalizedQuery.isBlank()) {
      return 0
    }

    val haystack = content.lowercase()
    val needle = normalizedQuery.lowercase()
    val firstIndex = haystack.indexOf(needle)

    if (firstIndex < 0) {
      return 0
    }

    val count = countMatches(haystack, needle)
    currentEditor.caretModel.moveToOffset(firstIndex)
    currentEditor.selectionModel.setSelection(firstIndex, firstIndex + needle.length)
    currentEditor.scrollingModel.scrollToCaret(ScrollType.CENTER)
    return count
  }

  override fun dispose() {
    val currentEditor = editor ?: return
    remove(currentEditor.component)
    EditorFactory.getInstance().releaseEditor(currentEditor)
    editor = null
  }

  private fun replaceEditor(
    value: String,
    useJsonHighlighting: Boolean
  ) {
    editor?.let {
      remove(it.component)
      EditorFactory.getInstance().releaseEditor(it)
    }

    val document = EditorFactory.getInstance().createDocument(value)
    document.setReadOnly(true)

    val newEditor = EditorFactory.getInstance().createViewer(document, project) as EditorEx
    newEditor.highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(
      project,
      if (useJsonHighlighting) {
        FileTypeManager.getInstance().getFileTypeByExtension("json")
      } else {
        PlainTextFileType.INSTANCE
      }
    )
    newEditor.settings.isLineNumbersShown = true
    newEditor.settings.isFoldingOutlineShown = useJsonHighlighting
    newEditor.settings.additionalColumnsCount = 1
    newEditor.settings.additionalLinesCount = 1
    newEditor.settings.isCaretRowShown = false
    newEditor.setBorder(JBUI.Borders.empty())

    editor = newEditor
    add(newEditor.component, BorderLayout.CENTER)
    revalidate()
    repaint()
  }

  private fun countMatches(
    haystack: String,
    needle: String
  ): Int {
    var count = 0
    var startIndex = 0
    while (true) {
      val index = haystack.indexOf(needle, startIndex)
      if (index < 0) {
        return count
      }
      count += 1
      startIndex = index + needle.length
    }
  }
}
