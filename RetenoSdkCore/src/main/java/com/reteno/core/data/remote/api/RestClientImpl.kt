package com.reteno.core.data.remote.api

import com.reteno.core.BuildConfig
import com.reteno.core.RetenoConfig
import com.reteno.core.data.local.config.RestConfig
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.internal.log.LogPriority
import com.reteno.core.internal.log.Redact
import com.reteno.core.internal.log.RetenoLogSink
import com.reteno.core.internal.log.log
import com.reteno.core.util.Util
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URLEncoder

internal class RestClientImpl(
    private val restConfig: RestConfig,
    private val retenoConfigProvider: () -> RetenoConfig,
    private val logSink: RetenoLogSink,
    private val connections: HttpConnections = HttpsConnections
) : RestClient {

    override fun makeRequest(
        method: HttpMethod,
        apiContract: ApiContract,
        body: String?,
        headers: Map<String, String>?,
        queryParams: Map<String, String?>?,
        retryCount: Int,
        responseCallback: ResponseCallback
    ) {
        val response = try {
            exchange(method, apiContract, body, headers, queryParams)
        } catch (error: Exception) {
            logSink.log(LogPriority.DEBUG, TAG) { "${method.httpMethodName} failed: ${Redact.summary(error)}" }
            responseCallback.onFailure(null, null, error)
            return
        }
        logSink.log(LogPriority.INFO, TAG) { "${method.httpMethodName} → ${response.code}" }
        if (response.isRetryable && retryCount != 0) {
            makeRequest(method, apiContract, body, headers, queryParams, retryCount - 1, responseCallback)
            return
        }
        response.deliverTo(responseCallback)
    }

    private fun exchange(
        method: HttpMethod,
        apiContract: ApiContract,
        body: String?,
        headers: Map<String, String>?,
        queryParams: Map<String, String?>?
    ): HttpResponse {
        val connection = connections.open(withQuery(apiContract.url, queryParams))
        try {
            connection.prepare(method, apiContract, headers)
            if (body != null) {
                connection.doOutput = true
                write(connection.outputStream, body)
            }
            val code = connection.responseCode
            if (code == HTTP_OK || code == HTTP_MOVED_PERMANENTLY || code == HTTP_FOUND) {
                return Success(code, connection.headerFields, readText(connection.inputStream))
            }
            return Failure(code, connection.errorStream?.let { readText(it) })
        } finally {
            connection.disconnect()
        }
    }

    private fun HttpURLConnection.prepare(method: HttpMethod, apiContract: ApiContract, headers: Map<String, String>?) {
        if (Util.isDebugView()) {
            setRequestProperty(HEADER_DEBUG, "true")
        }
        if (apiContract is ApiContract.MobileApi || apiContract.identifiesDevice()) {
            val config = retenoConfigProvider()
            setRequestProperty(HEADER_KEY, config.accessKey)
            setRequestProperty(HEADER_VERSION, "${config.platform} ${BuildConfig.SDK_VERSION}")
        }
        if (apiContract.identifiesDevice()) {
            setRequestProperty(HEADER_DEVICE_ID, restConfig.deviceId.id)
        }
        setRequestProperty(HEADER_ACCEPT, HEADER_ACCEPT_VALUE)
        setRequestProperty(HEADER_CONTENT, HEADER_CONTENT_VALUE)
        if (method != HttpMethod.GET) {
            setRequestProperty(HEADER_ENCODING, HEADER_ENCODING_VALUE)
            setChunkedStreamingMode(0)
        }
        headers?.forEach { (key, value) -> setRequestProperty(key, value) }
        readTimeout = READ_TIMEOUT
        connectTimeout = TIMEOUT
        requestMethod = method.httpMethodName
        useCaches = false
        instanceFollowRedirects = true
    }

    private fun ApiContract.identifiesDevice(): Boolean =
        this is ApiContract.InAppMessages || this is ApiContract.AppInbox || this is ApiContract.Recommendation

    private fun withQuery(url: String, params: Map<String, String?>?): String {
        val query = params.orEmpty()
            .mapNotNull { (key, value) -> if (value == null) null else "${encode(key)}=${encode(value)}" }
        if (query.isEmpty()) return url
        val separator = if (url.contains('?')) "&" else "?"
        return query.joinToString(separator = "&", prefix = "$url$separator")
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    private fun write(stream: OutputStream, body: String) {
        val writer = OutputStreamWriter(stream, Charsets.UTF_8)
        try {
            writer.write(body)
        } finally {
            writer.close()
        }
    }

    private fun readText(stream: InputStream): String {
        val reader = InputStreamReader(stream, Charsets.UTF_8)
        try {
            return reader.readText()
        } finally {
            reader.close()
        }
    }

    private abstract class HttpResponse(val code: Int) {

        val isRetryable: Boolean
            get() = code >= HTTP_SERVER_ERROR || code == HTTP_CLIENT_TIMEOUT

        abstract fun deliverTo(callback: ResponseCallback)
    }

    private class Success(code: Int, val headers: Map<String, List<String>>, val body: String) : HttpResponse(code) {

        override fun deliverTo(callback: ResponseCallback) {
            callback.onSuccess(headers, body)
        }
    }

    private class Failure(code: Int, val body: String?) : HttpResponse(code) {

        override fun deliverTo(callback: ResponseCallback) {
            callback.onFailure(code, body, null)
        }
    }

    companion object {
        private const val TAG = "RestClientImpl"

        /** TIMEOUT should be not more than @see [com.reteno.core.internal.action.sync.DataSync.REGULAR_DELAY]
         * to prevent adding new operations while REST queries are ongoing
         **/
        private const val TIMEOUT = 10_000
        private const val READ_TIMEOUT = 15_000

        private const val HTTP_OK = 200
        private const val HTTP_MOVED_PERMANENTLY = 301
        private const val HTTP_FOUND = 302
        private const val HTTP_CLIENT_TIMEOUT = 408
        private const val HTTP_SERVER_ERROR = 500

        private const val HEADER_DEBUG = "X-Reteno-Debug"
        private const val HEADER_KEY = "X-Reteno-Access-Key"
        private const val HEADER_VERSION = "X-Reteno-SDK-Version"
        private const val HEADER_DEVICE_ID = "X-Reteno-Device-ID"
        private const val HEADER_CONTENT = "Content-Type"
        private const val HEADER_CONTENT_VALUE = "application/json; charset=UTF-8"
        private const val HEADER_ENCODING = "Accept-Encoding"
        private const val HEADER_ENCODING_VALUE = "gzip"
        private const val HEADER_ACCEPT = "Accept"
        private const val HEADER_ACCEPT_VALUE = "*/*"
        internal const val HEADER_X_AMZ_META_VERSION = "x-amz-meta-version"
    }
}
