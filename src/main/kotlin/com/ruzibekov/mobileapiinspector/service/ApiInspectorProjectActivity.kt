package com.ruzibekov.mobileapiinspector.service

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class ApiInspectorProjectActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    project.service<ApiInspectorServerService>().start()
  }
}
