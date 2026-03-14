package com.ruzibekov.mobileapiinspector.ui

import com.intellij.ide.CopyProvider
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.Disposer
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBUI
import com.ruzibekov.mobileapiinspector.model.ApiCallRecord
import com.ruzibekov.mobileapiinspector.service.ApiCallJsonParser
import com.ruzibekov.mobileapiinspector.service.ApiCallStoreService
import com.ruzibekov.mobileapiinspector.service.ApiInspectorConstants
import com.ruzibekov.mobileapiinspector.service.ApiInspectorServerService
import com.ruzibekov.mobileapiinspector.service.ApiInspectorSettingsService
import java.awt.BorderLayout
import java.awt.GridLayout
import java.awt.datatransfer.StringSelection
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.event.ChangeEvent
import javax.swing.event.DocumentEvent
import javax.swing.event.ListSelectionEvent

class ApiInspectorPanel(
  private val project: Project
) : JPanel(BorderLayout()), Disposable, CopyProvider {
  private val storeService = project.service<ApiCallStoreService>()
  private val serverService = project.service<ApiInspectorServerService>()

  private val allRecords = mutableListOf<ApiCallRecord>()
  private val filteredModel = DefaultListModel<ApiCallRecord>()
  private val list = JBList(filteredModel)
  private val listSearchField = JBTextField()
  private val sourceFilterCombo = ComboBox<ApiSourceFilter>()
  private val platformFilterCombo = ComboBox(ApiPlatformFilter.entries.toTypedArray())
  private val methodFilterCombo = ComboBox(ApiMethodFilter.entries.toTypedArray())
  private val statusFilterCombo = ComboBox(ApiStatusFilter.entries.toTypedArray())
  private val detailSearchField = JBTextField()
  private val detailSearchStatusLabel = JBLabel()
  private val connectionLabel = JBLabel()
  private val statusLabel = JBLabel()
  private val pauseButton = JButton("Pause")
  private val pinButton = JButton("Pin")
  private val clearButton = JButton("Clear")
  private val copyCurlButton = JButton("Copy cURL")
  private val copyJsonButton = JButton("Copy JSON")
  private val detailTabs = JBTabbedPane()
  private val overviewViewer = InspectorEditorViewer(project)
  private val headersViewer = InspectorEditorViewer(project)
  private val requestViewer = InspectorEditorViewer(project)
  private val requestTreeViewer = InspectorJsonTreeViewer()
  private val responseViewer = InspectorEditorViewer(project)
  private val responseTreeViewer = InspectorJsonTreeViewer()
  private val errorViewer = InspectorEditorViewer(project)
  private val settingsService = ApplicationManager.getApplication().service<ApiInspectorSettingsService>()

  init {
    serverService.start()
    border = JBUI.Borders.empty()
    Disposer.register(this, overviewViewer)
    Disposer.register(this, headersViewer)
    Disposer.register(this, requestViewer)
    Disposer.register(this, requestTreeViewer)
    Disposer.register(this, responseViewer)
    Disposer.register(this, responseTreeViewer)
    Disposer.register(this, errorViewer)
    settingsService.addListener(this) {
      ApplicationManager.getApplication().invokeLater {
        updateConnectionLabel()
      }
    }

    list.cellRenderer = ApiCallListCellRenderer().apply {
      isPinnedProvider = { storeService.isPinned(it.id) }
    }
    list.addListSelectionListener(::onSelectionChanged)

    listSearchField.emptyText.text = "Search requests"
    listSearchField.document.addDocumentListener(object : DocumentAdapter() {
      override fun textChanged(event: DocumentEvent) {
        applyFilter()
      }
    })
    sourceFilterCombo.addActionListener {
      applyFilter()
    }
    platformFilterCombo.addActionListener {
      applyFilter()
    }
    methodFilterCombo.addActionListener {
      applyFilter()
    }
    statusFilterCombo.addActionListener {
      applyFilter()
    }
    detailSearchField.emptyText.text = "Search details"
    detailSearchField.document.addDocumentListener(object : DocumentAdapter() {
      override fun textChanged(event: DocumentEvent) {
        applyDetailSearch()
      }
    })
    detailSearchStatusLabel.border = JBUI.Borders.emptyLeft(8)
    connectionLabel.border = JBUI.Borders.emptyRight(8)

    clearButton.addActionListener {
      storeService.clear()
      applyFilter()
    }
    pauseButton.addActionListener {
      storeService.setPaused(!storeService.isPaused())
      updatePauseButton()
      updateStatus()
    }
    pinButton.addActionListener {
      selectedRecord()?.let {
        storeService.togglePinned(it.id)
        updatePinButton(it)
        applyFilter()
      }
    }
    copyCurlButton.addActionListener {
      selectedRecord()?.let { copyToClipboard(buildCurl(it)) }
    }
    copyJsonButton.addActionListener {
      selectedRecord()?.let { copyToClipboard(buildRawJson(it)) }
    }

    detailTabs.addTab("Overview", overviewViewer)
    detailTabs.addTab("Headers", headersViewer)
    detailTabs.addTab("Request", requestViewer)
    detailTabs.addTab("Request Tree", requestTreeViewer)
    detailTabs.addTab("Response", responseViewer)
    detailTabs.addTab("Response Tree", responseTreeViewer)
    detailTabs.addTab("Error", errorViewer)
    detailTabs.addChangeListener(::onDetailTabChanged)

    val leftPanel = JPanel(BorderLayout())
    leftPanel.add(createListToolbar(), BorderLayout.NORTH)
    leftPanel.add(JBScrollPane(list), BorderLayout.CENTER)

    val rightPanel = JPanel(BorderLayout())
    rightPanel.add(createDetailToolbar(), BorderLayout.NORTH)
    rightPanel.add(detailTabs, BorderLayout.CENTER)

    val splitter = JBSplitter(false, 0.32f)
    splitter.firstComponent = leftPanel
    splitter.secondComponent = rightPanel

    add(splitter, BorderLayout.CENTER)
    add(statusLabel, BorderLayout.SOUTH)

    storeService.addListener(this) { snapshot ->
      ApplicationManager.getApplication().invokeLater {
        allRecords.clear()
        allRecords.addAll(snapshot)
        applyFilter()
      }
    }

    updateDetail(null)
    updateConnectionLabel()
    updatePauseButton()
    updateStatus()
  }

  override fun dispose() = Unit

  override fun performCopy(dataContext: com.intellij.openapi.actionSystem.DataContext) {
    selectedRecord()?.let { copyToClipboard(buildRawJson(it)) }
  }

  override fun isCopyEnabled(dataContext: com.intellij.openapi.actionSystem.DataContext): Boolean {
    return selectedRecord() != null
  }

  override fun isCopyVisible(dataContext: com.intellij.openapi.actionSystem.DataContext): Boolean {
    return true
  }

  private fun createDetailToolbar(): JPanel {
    val panel = JPanel()
    panel.layout = javax.swing.BoxLayout(panel, javax.swing.BoxLayout.X_AXIS)
    panel.border = JBUI.Borders.empty(8)
    panel.add(connectionLabel)
    panel.add(javax.swing.Box.createHorizontalStrut(JBUI.scale(12)))
    panel.add(detailSearchField)
    panel.add(detailSearchStatusLabel)
    panel.add(javax.swing.Box.createHorizontalGlue())
    panel.add(pinButton)
    panel.add(javax.swing.Box.createHorizontalStrut(JBUI.scale(8)))
    panel.add(pauseButton)
    panel.add(javax.swing.Box.createHorizontalStrut(JBUI.scale(8)))
    panel.add(copyCurlButton)
    panel.add(javax.swing.Box.createHorizontalStrut(JBUI.scale(8)))
    panel.add(copyJsonButton)
    panel.add(javax.swing.Box.createHorizontalStrut(JBUI.scale(8)))
    panel.add(clearButton)
    return panel
  }

  private fun createListToolbar(): JPanel {
    val container = JPanel(VerticalLayout(JBUI.scale(8)))
    container.border = JBUI.Borders.empty(8)
    container.add(listSearchField)

    val filtersPanel = JPanel(GridLayout(2, 2, JBUI.scale(8), JBUI.scale(8)))
    filtersPanel.add(sourceFilterCombo)
    filtersPanel.add(platformFilterCombo)
    filtersPanel.add(methodFilterCombo)
    filtersPanel.add(statusFilterCombo)
    container.add(filtersPanel)
    return container
  }

  private fun applyFilter() {
    rebuildSourceFilterItems()
    val query = listSearchField.text.trim()
    val selectedId = selectedRecord()?.id
    val sourceFilter = sourceFilterCombo.item as? ApiSourceFilter ?: ApiSourceFilter.All
    val platformFilter = platformFilterCombo.item as? ApiPlatformFilter ?: ApiPlatformFilter.ALL
    val methodFilter = methodFilterCombo.item as? ApiMethodFilter ?: ApiMethodFilter.ALL
    val statusFilter = statusFilterCombo.item as? ApiStatusFilter ?: ApiStatusFilter.ALL

    filteredModel.clear()

    ApiPinnedRecords.sort(
      allRecords
      .filter {
        ApiRecordFilter.matches(
          it,
          query,
          sourceFilter,
          platformFilter,
          methodFilter,
          statusFilter
        )
      }
      ,
      storeService.pinnedIdsSnapshot()
    ).forEach(filteredModel::addElement)

    if (filteredModel.isEmpty) {
      updateDetail(null)
    } else {
      val selectedIndex = (0 until filteredModel.size())
        .firstOrNull { filteredModel.getElementAt(it).id == selectedId }
        ?: 0
      list.selectedIndex = selectedIndex
      updateDetail(filteredModel.getElementAt(selectedIndex))
    }

    updateStatus()
  }

  private fun onSelectionChanged(event: ListSelectionEvent) {
    if (event.valueIsAdjusting) {
      return
    }

    updateDetail(selectedRecord())
    updateStatus()
  }

  private fun onDetailTabChanged(event: ChangeEvent) {
    applyDetailSearch()
  }

  private fun selectedRecord(): ApiCallRecord? {
    return list.selectedValue
  }

  private fun updateDetail(record: ApiCallRecord?) {
    if (record == null) {
      overviewViewer.setContent("Select a request", false)
      headersViewer.setContent("", false)
      requestViewer.setContent("", false)
      requestTreeViewer.setContent("", true)
      responseViewer.setContent("", false)
      responseTreeViewer.setContent("", true)
      errorViewer.setContent("", false)
      pinButton.isEnabled = false
      pinButton.text = "Pin"
      copyCurlButton.isEnabled = false
      copyJsonButton.isEnabled = false
      detailSearchStatusLabel.text = ""
      return
    }

    val requestBody = ApiCallJsonParser.formatJson(record.request.body)
    val responseBody = ApiCallJsonParser.formatJson(record.response?.body)

    overviewViewer.setContent(buildOverview(record), false)
    headersViewer.setContent(buildHeaders(record), true)
    requestViewer.setContent(requestBody, shouldUseJson(record.request.contentType, requestBody))
    requestTreeViewer.setContent(requestBody, true)
    responseViewer.setContent(
      responseBody,
      shouldUseJson(record.response?.contentType, responseBody)
    )
    responseTreeViewer.setContent(responseBody, true)
    errorViewer.setContent(buildError(record), false)
    updatePinButton(record)
    copyCurlButton.isEnabled = true
    copyJsonButton.isEnabled = true
    applyDetailSearch()
  }

  private fun buildOverview(record: ApiCallRecord): String {
    return buildString {
      appendLine("Platform: ${record.platform.uppercase()}")
      appendLine("Time: ${record.timestamp}")
      appendLine("Method: ${record.request.method}")
      appendLine("Path: ${record.request.path}")
      appendLine("URL: ${record.request.url}")
      appendLine("Duration: ${record.durationMs} ms")
      appendLine("Status: ${record.response?.statusCode ?: "ERROR"}")
      if (!record.response?.statusMessage.isNullOrBlank()) {
        appendLine("Status Message: ${record.response?.statusMessage}")
      }
      if (!record.request.contentType.isNullOrBlank()) {
        appendLine("Request Content-Type: ${record.request.contentType}")
      }
      if (!record.response?.contentType.isNullOrBlank()) {
        appendLine("Response Content-Type: ${record.response?.contentType}")
      }
      if (record.request.queryParameters.isNotEmpty()) {
        appendLine()
        appendLine("Query")
        appendLine(ApiCallJsonParser.formatJson(mapToJson(record.request.queryParameters)))
      }
    }
  }

  private fun buildHeaders(record: ApiCallRecord): String {
    return ApiCallJsonParser.formatJson(
      mapToJson(
        mapOf(
          "requestHeaders" to record.request.headers,
          "responseHeaders" to (record.response?.headers ?: emptyMap<String, String>())
        )
      )
    )
  }

  private fun buildError(record: ApiCallRecord): String {
    val error = record.error ?: return ""
    return "Type: ${error.type}\nMessage: ${error.message}"
  }

  private fun buildCurl(record: ApiCallRecord): String {
    val builder = StringBuilder()
    builder.append("curl -X ${record.request.method}")
    record.request.headers.forEach { (key, value) ->
      builder.append(" -H ")
      builder.append(shellQuote("$key: $value"))
    }
    record.request.body?.takeIf { it.isNotBlank() }?.let {
      builder.append(" --data ")
      builder.append(shellQuote(it))
    }
    builder.append(" ")
    builder.append(shellQuote(record.request.url))
    return builder.toString()
  }

  private fun buildRawJson(record: ApiCallRecord): String {
    return ApiCallJsonParser.formatJson(
      buildString {
        append("{")
        append("\"id\":\"${escape(record.id)}\",")
        append("\"platform\":\"${escape(record.platform)}\",")
        append("\"timestamp\":\"${escape(record.timestamp)}\",")
        append("\"request\":${mapToJson(mapOf(
          "method" to record.request.method,
          "url" to record.request.url,
          "path" to record.request.path,
          "headers" to record.request.headers,
          "queryParameters" to record.request.queryParameters,
          "body" to (record.request.body ?: ""),
          "contentType" to (record.request.contentType ?: "")
        ))},")
        append("\"response\":${mapToJson(mapOf(
          "statusCode" to (record.response?.statusCode?.toString() ?: ""),
          "statusMessage" to (record.response?.statusMessage ?: ""),
          "headers" to (record.response?.headers ?: emptyMap<String, String>()),
          "body" to (record.response?.body ?: ""),
          "contentType" to (record.response?.contentType ?: "")
        ))},")
        append("\"error\":${mapToJson(mapOf(
          "type" to (record.error?.type ?: ""),
          "message" to (record.error?.message ?: "")
        ))},")
        append("\"durationMs\":${record.durationMs}")
        append("}")
      }
    )
  }

  private fun copyToClipboard(value: String) {
    CopyPasteManager.getInstance().setContents(StringSelection(value))
  }

  private fun applyDetailSearch() {
    val viewer = selectedDetailViewer() ?: return
    val query = detailSearchField.text.trim()
    val matchesCount = viewer.highlight(query)
    detailSearchStatusLabel.text = when {
      query.isBlank() -> ""
      matchesCount <= 0 -> "No match"
      matchesCount == 1 -> "1 match"
      else -> "$matchesCount matches"
    }
  }

  private fun updateStatus() {
    val selectedText = selectedRecord()?.let { " • ${it.request.method} ${it.title}" }.orEmpty()
    val pausedText = if (storeService.isPaused()) " • Paused" else ""
    val pinnedCount = storeService.pinnedIdsSnapshot().size
    val pinnedText = if (pinnedCount > 0) " • Pinned: $pinnedCount" else ""
    statusLabel.text = "Requests: ${filteredModel.size()} / ${allRecords.size}$selectedText$pausedText$pinnedText"
  }

  private fun selectedDetailViewer(): InspectorDetailViewer? {
    return when (detailTabs.selectedIndex) {
      0 -> overviewViewer
      1 -> headersViewer
      2 -> requestViewer
      3 -> requestTreeViewer
      4 -> responseViewer
      5 -> responseTreeViewer
      6 -> errorViewer
      else -> null
    }
  }

  private fun updatePauseButton() {
    pauseButton.text = if (storeService.isPaused()) "Resume" else "Pause"
  }

  private fun rebuildSourceFilterItems() {
    val selectedFilter = sourceFilterCombo.item as? ApiSourceFilter ?: ApiSourceFilter.All
    val items = buildList {
      add(ApiSourceFilter.All)
      allRecords
        .map { ApiSourceFilter.Item(it.sourceId, it.sourceName) }
        .distinctBy { it.sourceId }
        .sortedBy { it.sourceName.lowercase() }
        .forEach(::add)
    }

    val selectedSourceId = (selectedFilter as? ApiSourceFilter.Item)?.sourceId
    sourceFilterCombo.removeAllItems()
    items.forEach(sourceFilterCombo::addItem)
    val restoredItem = items.firstOrNull {
      (it as? ApiSourceFilter.Item)?.sourceId == selectedSourceId
    } ?: ApiSourceFilter.All
    sourceFilterCombo.selectedItem = restoredItem
  }

  private fun updateConnectionLabel() {
    connectionLabel.text = "${serverService.host}:${serverService.port}"
  }

  private fun updatePinButton(record: ApiCallRecord) {
    pinButton.isEnabled = true
    pinButton.text = if (storeService.isPinned(record.id)) "Unpin" else "Pin"
  }

  private fun shouldUseJson(
    contentType: String?,
    content: String
  ): Boolean {
    if (content.isBlank()) {
      return false
    }

    if (contentType?.contains("json", ignoreCase = true) == true) {
      return true
    }

    val trimmedContent = content.trim()
    return trimmedContent.startsWith("{") || trimmedContent.startsWith("[")
  }

  private fun shellQuote(value: String): String {
    return "'${value.replace("'", "'\\''")}'"
  }

  private fun escape(value: String): String {
    return value
      .replace("\\", "\\\\")
      .replace("\"", "\\\"")
      .replace("\n", "\\n")
  }

  private fun mapToJson(value: Any): String {
    return when (value) {
      is Map<*, *> -> {
        value.entries.joinToString(prefix = "{", postfix = "}") { entry ->
          "\"${escape(entry.key.toString())}\":${mapToJson(entry.value ?: "")}"
        }
      }
      is String -> "\"${escape(value)}\""
      else -> "\"${escape(value.toString())}\""
    }
  }
}
