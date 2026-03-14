const defaultSettings = {
  enabled: true,
  host: '127.0.0.1',
  port: 63345,
  portScanCount: 16,
  platform: 'web',
  sourceId: 'web-app',
  sourceName: 'Web App',
  environment: 'local',
}

const sensitiveKeys = ['authorization', 'token', 'secret', 'cookie', 'session', 'password']
let resolvedEndpoint = null
let endpointResolution = null

export function installApiInspector(userSettings = {}) {
  if (typeof window === 'undefined') {
    return createNoopController()
  }

  if (window.__apiInspectorController) {
    return window.__apiInspectorController
  }

  const settings = {
    ...defaultSettings,
    ...userSettings,
  }

  if (!settings.enabled) {
    return createNoopController()
  }

  const originalFetch = typeof window.fetch === 'function' ? window.fetch.bind(window) : null
  const OriginalXMLHttpRequest = window.XMLHttpRequest

  if (originalFetch == null || OriginalXMLHttpRequest == null) {
    return createNoopController()
  }

  const controller = {
    dispose() {
      window.fetch = originalFetch
      window.XMLHttpRequest = OriginalXMLHttpRequest
      delete window.__apiInspectorController
    },
  }

  window.__apiInspectorController = controller

  window.fetch = async function apiInspectorFetch(input, init) {
    const request = input instanceof Request ? input : new Request(input, init)
    const url = resolveUrl(request.url)
    if (shouldSkip(url, settings)) {
      return originalFetch(input, init)
    }

    const requestBody = await readRequestBody(request, init)
    const startedAt = Date.now()

    try {
      const response = await originalFetch(request)
      const responseBody = await readResponseBody(response)
      sendEvent(
        originalFetch,
        settings,
        buildEvent({
          method: request.method,
          url,
          requestHeaders: sanitizeObject(headersToObject(request.headers)),
          queryParameters: sanitizeObject(urlToQuery(url)),
          requestBody,
          requestContentType: request.headers.get('content-type'),
          responseStatusCode: response.status,
          responseStatusMessage: response.statusText,
          responseHeaders: sanitizeObject(headersToObject(response.headers)),
          responseBody,
          responseContentType: response.headers.get('content-type'),
          durationMs: Date.now() - startedAt,
        })
      )
      return response
    } catch (error) {
      sendEvent(
        originalFetch,
        settings,
        buildEvent({
          method: request.method,
          url,
          requestHeaders: sanitizeObject(headersToObject(request.headers)),
          queryParameters: sanitizeObject(urlToQuery(url)),
          requestBody,
          requestContentType: request.headers.get('content-type'),
          durationMs: Date.now() - startedAt,
          error: {
            type: 'fetch',
            message: error instanceof Error ? error.message : String(error),
          },
        })
      )
      throw error
    }
  }

  window.XMLHttpRequest = createPatchedXmlHttpRequest({
    OriginalXMLHttpRequest,
    originalFetch,
    settings,
  })

  return controller
}

function createNoopController() {
  return {
    dispose() {},
  }
}

function createPatchedXmlHttpRequest({
  OriginalXMLHttpRequest,
  originalFetch,
  settings,
}) {
  return function ApiInspectorXMLHttpRequest() {
    const xhr = new OriginalXMLHttpRequest()
    let method = 'GET'
    let url = ''
    let requestBody = null
    const requestHeaders = {}
    let startedAt = 0
    let errorMessage = ''

    const originalOpen = xhr.open
    xhr.open = function patchedOpen(nextMethod, nextUrl, ...rest) {
      method = String(nextMethod || 'GET').toUpperCase()
      url = resolveUrl(nextUrl)
      return originalOpen.call(xhr, nextMethod, nextUrl, ...rest)
    }

    const originalSetRequestHeader = xhr.setRequestHeader
    xhr.setRequestHeader = function patchedSetRequestHeader(name, value) {
      requestHeaders[name] = value
      return originalSetRequestHeader.call(xhr, name, value)
    }

    xhr.addEventListener('error', () => {
      errorMessage = 'Network error'
    })

    const originalSend = xhr.send
    xhr.send = function patchedSend(body) {
      if (shouldSkip(url, settings)) {
        return originalSend.call(xhr, body)
      }

      requestBody = serializeBody(body)
      startedAt = Date.now()

      xhr.addEventListener(
        'loadend',
        () => {
          const responseHeaders = sanitizeObject(parseResponseHeaders(xhr.getAllResponseHeaders()))
          const responseBody = sanitizeBodyString(xhr.responseText)
          const payload = buildEvent({
            method,
            url,
            requestHeaders: sanitizeObject(requestHeaders),
            queryParameters: sanitizeObject(urlToQuery(url)),
            requestBody,
            requestContentType: requestHeaders['Content-Type'] || requestHeaders['content-type'] || null,
            responseStatusCode: xhr.status || null,
            responseStatusMessage: xhr.statusText || '',
            responseHeaders,
            responseBody,
            responseContentType: responseHeaders['content-type'] || null,
            durationMs: Date.now() - startedAt,
            error: xhr.status === 0 ? { type: 'xhr', message: errorMessage || 'Request failed' } : null,
          })
          sendEvent(originalFetch, settings, payload)
        },
        { once: true }
      )

      return originalSend.call(xhr, body)
    }

    return xhr
  }
}

function shouldSkip(url, settings) {
  const parsedUrl = new URL(url, window.location.href)
  const isInspectorPath = parsedUrl.pathname.startsWith('/api/inspector/')
  const isLocalInspectorHost =
    parsedUrl.hostname === settings.host ||
    parsedUrl.hostname === '127.0.0.1' ||
    parsedUrl.hostname === 'localhost'
  return isInspectorPath && isLocalInspectorHost
}

function buildEvent({
  method,
  url,
  requestHeaders,
  queryParameters,
  requestBody,
  requestContentType,
  responseStatusCode = null,
  responseStatusMessage = '',
  responseHeaders = {},
  responseBody = null,
  responseContentType = null,
  durationMs,
  error = null,
}) {
  const parsedUrl = new URL(url, window.location.href)
  return {
    id: createId(),
    platform: 'web',
    sourceId: settings.sourceId,
    sourceName: settings.sourceName,
    environment: settings.environment,
    timestamp: new Date().toISOString(),
    durationMs,
    request: {
      method,
      url: parsedUrl.toString(),
      path: parsedUrl.pathname,
      headers: requestHeaders,
      queryParameters,
      body: requestBody,
      contentType: requestContentType,
    },
    response: responseStatusCode == null
      ? null
      : {
          statusCode: responseStatusCode,
          statusMessage: responseStatusMessage,
          headers: responseHeaders,
          body: responseBody,
          contentType: responseContentType,
        },
    error,
  }
}

async function sendEvent(originalFetch, settings, payload) {
  const endpoint = await resolveEndpoint(originalFetch, settings)
  if (endpoint == null) {
    return
  }

  try {
    await postEvent(originalFetch, endpoint, payload)
  } catch {
    resolvedEndpoint = null
    const fallbackEndpoint = await resolveEndpoint(originalFetch, settings, true)
    if (fallbackEndpoint == null) {
      return
    }
    await postEvent(originalFetch, fallbackEndpoint, payload).catch(() => undefined)
  }
}

function resolveUrl(value) {
  return new URL(String(value), window.location.href).toString()
}

async function resolveEndpoint(originalFetch, settings, forceRefresh = false) {
  if (!forceRefresh && resolvedEndpoint != null) {
    return resolvedEndpoint
  }

  if (!forceRefresh && endpointResolution != null) {
    return endpointResolution
  }

  endpointResolution = discoverEndpoint(originalFetch, settings)
  const endpoint = await endpointResolution
  if (endpointResolution != null) {
    endpointResolution = null
  }
  if (endpoint != null) {
    resolvedEndpoint = endpoint
  }
  return endpoint
}

async function discoverEndpoint(originalFetch, settings) {
  for (const port of buildCandidatePorts(settings.port, settings.portScanCount)) {
    const healthUrl = `http://${settings.host}:${port}/api/inspector/health`
    try {
      const response = await originalFetch(healthUrl, {
        method: 'GET',
        mode: 'cors',
        cache: 'no-store',
      })
      if (response.ok) {
        return `http://${settings.host}:${port}/api/inspector/events`
      }
    } catch {
      continue
    }
  }

  return null
}

function buildCandidatePorts(basePort, portScanCount) {
  const ports = []
  for (let offset = 0; offset < portScanCount; offset += 1) {
    const port = basePort + offset
    if (port > 65535) {
      break
    }
    ports.push(port)
  }
  return ports
}

function postEvent(originalFetch, endpoint, payload) {
  return originalFetch(endpoint, {
    method: 'POST',
    mode: 'cors',
    keepalive: true,
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  })
}

async function readRequestBody(request, init) {
  if (init != null && init.body !== undefined) {
    return serializeBody(init.body)
  }

  try {
    const clonedRequest = request.clone()
    const text = await clonedRequest.text()
    return sanitizeBodyString(text)
  } catch {
    return null
  }
}

async function readResponseBody(response) {
  try {
    const clonedResponse = response.clone()
    const text = await clonedResponse.text()
    return sanitizeBodyString(text)
  } catch {
    return null
  }
}

function serializeBody(body) {
  if (body == null) {
    return null
  }

  if (typeof body === 'string') {
    return sanitizeBodyString(body)
  }

  if (body instanceof URLSearchParams) {
    return JSON.stringify(sanitizeObject(Object.fromEntries(body.entries())))
  }

  if (body instanceof FormData) {
    return JSON.stringify(sanitizeObject(Object.fromEntries(body.entries())))
  }

  if (body instanceof Blob || body instanceof ArrayBuffer) {
    return '[Binary payload]'
  }

  if (typeof body === 'object') {
    return JSON.stringify(sanitizeObject(body))
  }

  return String(body)
}

function sanitizeBodyString(value) {
  const trimmedValue = value.trim()
  if (trimmedValue.length === 0) {
    return ''
  }

  try {
    return JSON.stringify(sanitizeObject(JSON.parse(trimmedValue)))
  } catch {
    return value
  }
}

function sanitizeObject(value) {
  if (Array.isArray(value)) {
    return value.map((item) => sanitizeObject(item))
  }

  if (value != null && typeof value === 'object') {
    return Object.entries(value).reduce((accumulator, [key, nextValue]) => {
      accumulator[key] = isSensitiveKey(key) ? '***' : sanitizeObject(nextValue)
      return accumulator
    }, {})
  }

  return value
}

function isSensitiveKey(key) {
  const normalizedKey = key.toLowerCase()
  return sensitiveKeys.some((sensitiveKey) => normalizedKey.includes(sensitiveKey))
}

function headersToObject(headers) {
  return Object.fromEntries(headers.entries())
}

function urlToQuery(url) {
  const parsedUrl = new URL(url, window.location.href)
  return Object.fromEntries(parsedUrl.searchParams.entries())
}

function parseResponseHeaders(rawHeaders) {
  return rawHeaders
    .trim()
    .split(/[\r\n]+/)
    .filter((value) => value.length > 0)
    .reduce((accumulator, line) => {
      const separatorIndex = line.indexOf(':')
      if (separatorIndex < 0) {
        return accumulator
      }
      const key = line.slice(0, separatorIndex).trim()
      const value = line.slice(separatorIndex + 1).trim()
      accumulator[key] = value
      return accumulator
    }, {})
}

function createId() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }

  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

export default installApiInspector
