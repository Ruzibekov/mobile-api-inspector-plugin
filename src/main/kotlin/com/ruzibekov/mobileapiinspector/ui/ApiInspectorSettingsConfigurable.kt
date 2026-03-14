package com.ruzibekov.mobileapiinspector.ui

import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.ruzibekov.mobileapiinspector.service.ApiInspectorServerService
import com.ruzibekov.mobileapiinspector.service.ApiInspectorSettingsService
import javax.swing.JComponent
import javax.swing.JPanel

class ApiInspectorSettingsConfigurable : SearchableConfigurable {
  private val settingsService = service<ApiInspectorSettingsService>()

  private var panel: JPanel? = null
  private var hostField: JBTextField? = null
  private var portField: JBTextField? = null

  override fun getId(): String {
    return "mobileApiInspector.settings"
  }

  override fun getDisplayName(): String {
    return "API Inspector"
  }

  override fun createComponent(): JComponent {
    if (panel == null) {
      hostField = JBTextField()
      portField = JBTextField()
      panel = FormBuilder.createFormBuilder()
        .addLabeledComponent("Host", hostField!!)
        .addLabeledComponent("Preferred port", portField!!)
        .panel
    }

    reset()
    return panel!!
  }

  override fun isModified(): Boolean {
    val currentHost = hostField?.text?.trim().orEmpty()
    val currentPort = portField?.text?.trim().orEmpty()
    return currentHost != settingsService.host || currentPort != settingsService.port.toString()
  }

  @Throws(ConfigurationException::class)
  override fun apply() {
    val nextHost = hostField?.text?.trim().orEmpty()
    val nextPort = portField?.text?.trim().orEmpty().toIntOrNull()

    if (nextHost.isBlank()) {
      throw ConfigurationException("Host bo'sh bo'lmasligi kerak")
    }

    if (nextPort == null || nextPort !in 1..65535) {
      throw ConfigurationException("Port 1 dan 65535 gacha bo'lishi kerak")
    }

    settingsService.update(nextHost, nextPort)

    ProjectManager.getInstance().openProjects.forEach { project ->
      project.service<ApiInspectorServerService>()
        .restart()
        .exceptionOrNull()
        ?.let { error ->
          throw ConfigurationException(error.message ?: "API Inspector server qayta ishga tushmadi")
        }
    }
  }

  override fun reset() {
    hostField?.text = settingsService.host
    portField?.text = settingsService.port.toString()
  }

  override fun disposeUIResources() {
    panel = null
    hostField = null
    portField = null
  }
}
