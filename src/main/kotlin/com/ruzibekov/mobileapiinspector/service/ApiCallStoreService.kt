package com.ruzibekov.mobileapiinspector.service

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.util.Disposer
import com.ruzibekov.mobileapiinspector.model.ApiCallRecord
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.CopyOnWriteArrayList

@Service(Service.Level.PROJECT)
class ApiCallStoreService : Disposable {
  private val listeners = CopyOnWriteArrayList<(List<ApiCallRecord>) -> Unit>()
  private val records = mutableListOf<ApiCallRecord>()
  private val pinnedIds = linkedSetOf<String>()
  private val stateLock = Any()
  private val isCapturePaused = AtomicBoolean(false)

  fun isPaused(): Boolean {
    return isCapturePaused.get()
  }

  fun setPaused(value: Boolean) {
    isCapturePaused.set(value)
  }

  fun pinnedIdsSnapshot(): Set<String> {
    synchronized(stateLock) {
      return pinnedIds.toSet()
    }
  }

  fun isPinned(recordId: String): Boolean {
    synchronized(stateLock) {
      return pinnedIds.contains(recordId)
    }
  }

  fun togglePinned(recordId: String) {
    synchronized(stateLock) {
      if (!pinnedIds.add(recordId)) {
        pinnedIds.remove(recordId)
      }
    }
  }

  fun snapshot(): List<ApiCallRecord> {
    synchronized(stateLock) {
      return records.toList()
    }
  }

  fun addRecord(record: ApiCallRecord) {
    if (isPaused()) {
      return
    }

    val snapshot = synchronized(stateLock) {
      records.add(0, record)
      records.toList()
    }
    notifyListeners(snapshot)
  }

  fun clear() {
    val snapshot = synchronized(stateLock) {
      records.clear()
      records.toList()
    }
    notifyListeners(snapshot)
  }

  fun addListener(
    parentDisposable: Disposable,
    listener: (List<ApiCallRecord>) -> Unit
  ) {
    listeners += listener
    listener(snapshot())

    Disposer.register(parentDisposable) {
      listeners.remove(listener)
    }
  }

  override fun dispose() {
    listeners.clear()
    synchronized(stateLock) {
      records.clear()
      pinnedIds.clear()
    }
  }

  private fun notifyListeners(snapshot: List<ApiCallRecord>) {
    listeners.forEach { it(snapshot) }
  }
}
