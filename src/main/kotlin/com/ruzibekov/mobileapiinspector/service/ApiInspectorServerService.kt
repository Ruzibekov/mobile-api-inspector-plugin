package com.ruzibekov.mobileapiinspector.service

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.ruzibekov.mobileapiinspector.model.ApiCallRecord
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.OutputStream
import java.net.BindException
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

@Service(Service.Level.PROJECT)
class ApiInspectorServerService(
  private val project: Project
) : Disposable {
  private val settingsService = service<ApiInspectorSettingsService>()
  private val stateLock = Any()
  private var server: HttpServer? = null
  private var currentHost: String? = null
  private var currentPort: Int? = null

  val port: Int
    get() = currentPort ?: settingsService.port

  val host: String
    get() = currentHost ?: settingsService.host

  fun start() {
    restart().onFailure { error ->
      thisLogger().warn("API Inspector server start failed", error)
    }
  }

  fun restart(): Result<Unit> {
    synchronized(stateLock) {
      val nextHost = settingsService.host
      val nextPort = settingsService.port

      if (server != null && currentHost == nextHost && currentPort == nextPort) {
        return Result.success(Unit)
      }

      stopServer()

      return runCatching {
        val newServer = createServer(nextHost, nextPort)

        newServer.createContext(ApiInspectorConstants.healthPath) { exchange ->
          if (exchange.requestMethod == "OPTIONS") {
            respond(exchange, 204, "")
            return@createContext
          }
          respond(
            exchange,
            200,
            """{"status":"ok","host":"${host}","port":${port}}"""
          )
        }

        newServer.createContext(ApiInspectorConstants.eventsPath) { exchange ->
          if (exchange.requestMethod == "OPTIONS") {
            respond(exchange, 204, "")
            return@createContext
          }

          if (exchange.requestMethod != "POST") {
            respond(exchange, 405, """{"error":"Method Not Allowed"}""")
            return@createContext
          }

          val body = exchange.requestBody.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }

          runCatching {
            val record: ApiCallRecord = ApiCallJsonParser.parse(body)
            project.service<ApiCallStoreService>().addRecord(record)
          }.onSuccess {
            respond(exchange, 202, """{"accepted":true}""")
          }.onFailure {
            respond(exchange, 400, """{"accepted":false}""")
          }
        }

        newServer.executor = Executors.newSingleThreadExecutor()
        newServer.start()
        server = newServer
        currentHost = nextHost
        currentPort = nextPort
      }
    }
  }

  override fun dispose() {
    synchronized(stateLock) {
      stopServer()
    }
  }

  private fun respond(
    exchange: HttpExchange,
    statusCode: Int,
    body: String
  ) {
    val bytes = body.toByteArray(StandardCharsets.UTF_8)
    exchange.responseHeaders.add("Content-Type", "application/json")
    exchange.responseHeaders.add("Access-Control-Allow-Origin", "*")
    exchange.responseHeaders.add("Access-Control-Allow-Headers", "Content-Type, Authorization")
    exchange.responseHeaders.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
    exchange.sendResponseHeaders(statusCode, bytes.size.toLong())
    exchange.responseBody.use { output: OutputStream ->
      output.write(bytes)
    }
  }

  private fun stopServer() {
    server?.stop(0)
    server = null
    currentHost = null
    currentPort = null
  }

  private fun createServer(
    host: String,
    preferredPort: Int
  ): HttpServer {
    var lastError: BindException? = null

    for (port in ApiInspectorPortResolver.candidatePorts(preferredPort)) {
      try {
        return HttpServer.create(InetSocketAddress(host, port), 0).also {
          currentHost = host
          currentPort = port
        }
      } catch (error: BindException) {
        lastError = error
      }
    }

    throw lastError ?: BindException("API Inspector uchun bo'sh port topilmadi")
  }
}
