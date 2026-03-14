package com.ruzibekov.mobileapiinspector.ui

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.ruzibekov.mobileapiinspector.model.ApiCallRecord
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer

class ApiCallListCellRenderer : JPanel(BorderLayout()), ListCellRenderer<ApiCallRecord> {
  private val titleLabel = JLabel()
  private val subtitleLabel = JLabel()
  private val methodLabel = JLabel()
  private val pinLabel = JLabel()

  var isPinnedProvider: (ApiCallRecord) -> Boolean = { false }

  init {
    border = BorderFactory.createEmptyBorder(JBUI.scale(8), JBUI.scale(10), JBUI.scale(8), JBUI.scale(10))
    titleLabel.font = titleLabel.font.deriveFont(Font.BOLD.toFloat())
    subtitleLabel.foreground = JBColor.GRAY
    methodLabel.font = methodLabel.font.deriveFont(Font.BOLD.toFloat())
    pinLabel.font = pinLabel.font.deriveFont(Font.BOLD.toFloat())

    val leftPanel = JPanel(BorderLayout())
    leftPanel.isOpaque = false
    leftPanel.add(methodLabel, BorderLayout.WEST)
    leftPanel.add(titleLabel, BorderLayout.CENTER)
    leftPanel.add(pinLabel, BorderLayout.EAST)

    add(leftPanel, BorderLayout.NORTH)
    add(subtitleLabel, BorderLayout.SOUTH)
    isOpaque = true
  }

  override fun getListCellRendererComponent(
    list: JList<out ApiCallRecord>,
    value: ApiCallRecord,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean
  ): Component {
    methodLabel.text = "${value.request.method}  "
    titleLabel.text = value.title
    subtitleLabel.text = value.subtitle
    pinLabel.text = if (isPinnedProvider(value)) "★" else ""

    background = if (isSelected) list.selectionBackground else list.background
    titleLabel.foreground = if (isSelected) list.selectionForeground else list.foreground
    methodLabel.foreground = titleLabel.foreground
    pinLabel.foreground = if (isSelected) list.selectionForeground else JBColor(0xC58F00, 0xF5C542)
    subtitleLabel.foreground = if (isSelected) list.selectionForeground else JBColor.GRAY

    return this
  }
}
