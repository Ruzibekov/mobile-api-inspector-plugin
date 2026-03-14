package com.ruzibekov.mobileapiinspector.service

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.util.Disposer
import java.util.concurrent.CopyOnWriteArrayList

@State(
  name = "ApiInspectorSettings",
  storages = [Storage("mobile-api-inspector.xml")]
)
@Service(Service.Level.APP)
class ApiInspectorSettingsService : PersistentStateComponent<ApiInspectorSettingsState> {
  private var state = ApiInspectorSettingsState()
  private val listeners = CopyOnWriteArrayList<() -> Unit>()

  val host: String
    get() = state.host.ifBlank { ApiInspectorConstants.defaultHost }

  val port: Int
    get() = if (state.port in 1..65535) state.port else ApiInspectorConstants.defaultPort

  override fun getState(): ApiInspectorSettingsState {
    return state
  }

  override fun loadState(state: ApiInspectorSettingsState) {
    this.state = state
    notifyListeners()
  }

  fun update(
    host: String,
    port: Int
  ) {
    state = ApiInspectorSettingsState(host = host.trim(), port = port)
    notifyListeners()
  }

  fun addListener(
    parentDisposable: Disposable,
    listener: () -> Unit
  ) {
    listeners += listener
    Disposer.register(parentDisposable) {
      listeners.remove(listener)
    }
  }

  private fun notifyListeners() {
    listeners.forEach { it() }
  }
}

data class ApiInspectorSettingsState(
  var host: String = ApiInspectorConstants.defaultHost,
  var port: Int = ApiInspectorConstants.defaultPort
)
