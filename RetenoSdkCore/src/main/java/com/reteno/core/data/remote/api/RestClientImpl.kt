package com.reteno.core.data.remote.api

import android.net.Uri
import com.reteno.core.BuildConfig
import com.reteno.core.data.local.config.RestConfig
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.util.Logger
import com.reteno.core.util.Util
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.util.zip.GZIPOutputStream
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

internal class RestClientImpl(
    private val restConfig: RestConfig,
    private val platform: String
) : RestClient {

    companion object {
        private val TAG = RestClientImpl::class.java.simpleName

        /** TIMEOUT should be not more than @see [com.reteno.core.domain.controller.ScheduleController.REGULAR_DELAY]
         * to prevent adding new operations while REST queries are ongoing
         **/
        private const val TIMEOUT = 10_000

        private const val READ_TIMEOUT = 15_000

        private const val HEADER_DEBUG = "X-Reteno-Debug"

        private const val HEADER_KEY = "X-Reteno-Access-Key"
        private const val HEADER_VERSION = "X-Reteno-SDK-Version"
        private const val HEADER_DEVICE_ID = "X-Reteno-Device-ID"

        private const val HEADER_CONTENT = "Content-Type"
        private const val HEADER_CONTENT_VALUE = "application/json; charset=UTF-8"
        private const val HEADER_ENCODING = "Accept-Encoding"
        private const val HEADER_CONTENT_ENCODING = "Content-Encoding"
        private const val HEADER_ENCODING_VALUE = "gzip"
        private const val HEADER_ACCEPT = "Accept"
        private const val HEADER_ACCEPT_VALUE = "*/*"
        internal const val HEADER_X_AMZ_META_VERSION = "x-amz-meta-version"
    }


    /**
     *  Perform Http request with
     *
     *  @param method HttpMethod type
     *  @param url base url + endpoint wrapped in ApiContract
     *  @param body JSON body for POST and PUT methods
     *  @param queryParams params for GET method
     *  @param responseCallback
     *
     *  @see com.reteno.data.remote.api.HttpMethod
     *  @see com.reteno.data.remote.api.ApiContract
     *  @see com.reteno.domain.ResponseCallback
     */
    override fun makeRequest(
        method: HttpMethod,
        apiContract: ApiContract,
        body: String?,
        headers: Map<String, String>?,
        queryParams: Map<String, String?>?,
        retryCount: Int,
        responseCallback: ResponseCallback
    ) {
        var urlConnection: HttpURLConnection? = null

        try {
            val urlWithParams = generateUrl(apiContract.url, queryParams)
            /*@formatter:off*/ Logger.i(TAG, "makeRequest(): ", "method = [" , method.httpMethodName , "], apiContract = [" , apiContract.url , "], body = [" , body , "], queryParams = [" , queryParams , "], responseCallback = [" , responseCallback , "]")
            /*@formatter:on*/
            urlConnection =
                defaultHttpConnection(method, urlWithParams, headers, apiContract)
            Logger.i(TAG, "makeRequest(): ", "request, headers: ${urlConnection.requestProperties}")

            if (body != null) {
                attachBody(urlConnection, body)
            }
            urlConnection.connect()
            val headerFiles: Map<String, List<String>> = urlConnection.headerFields
            Logger.i(TAG, "makeRequest(): ", "connect, headers: $headers")

            val responseCode = urlConnection.responseCode
            Logger.i(TAG, "makeRequest(): ", "responseCode: ", responseCode)

            when {
                responseCode == 200 -> {
                    val response = urlConnection.inputStream.bufferedReader().use { it.readText() }
                    Logger.i(TAG, "makeRequest(): ", "response: ", response)
                    responseCallback.onSuccess(headerFiles, response)
                }

                responseCode == 301 || responseCode == 302 -> {
                    val response = urlConnection.inputStream.bufferedReader().use { it.readText() }
                    Logger.i(TAG, "makeRequest(): ", "response: ", response)
                    responseCallback.onSuccess(headerFiles, response)
                }
                (responseCode >= 500 || responseCode == 408) && retryCount != 0 -> {
                    makeRequest(
                        method,
                        apiContract,
                        body,
                        headers,
                        queryParams,
                        retryCount - 1,
                        responseCallback
                    )
                }
                else -> {
                    val response =
                        urlConnection.errorStream?.bufferedReader()?.use { it.readText() }
                    Logger.i(TAG, "makeRequest(): ", "response: ", response)
                    responseCallback.onFailure(responseCode, response, null)
                }
            }

        } catch (e: Exception) {
            val errorMessages = """
                    m: $method,
                    u: ${apiContract.url},
                    e: ${e.message}
                """.trimIndent()
            Logger.d(TAG, "makeRequest(): ", errorMessages)
            responseCallback.onFailure(null, null, e)
        } finally {
            /*@formatter:off*/ Logger.i(TAG, "makeRequest(): ", "method = ", method.httpMethodName, "; apiContract = ", apiContract.url, "; status = disconnected") /*@formatter:on*/
            urlConnection?.disconnect()
        }
    }

    private fun defaultHttpConnection(
        method: HttpMethod,
        url: String,
        headers: Map<String, String>?,
        apiContract: ApiContract
    ): HttpURLConnection {
        val useSsl = url.startsWith("https") // TODO (bs) need to test
        val urlConnection = ConnectionManager.openConnection(url)

        urlConnection.apply {

            if ( Util.isDebugView()) {
                setRequestProperty(HEADER_DEBUG, "true")
            }

            when (apiContract) {
                is ApiContract.MobileApi -> {
                    setRequestProperty(HEADER_KEY, restConfig.accessKeyProvider())
                    setRequestProperty(HEADER_VERSION, "$platform ${BuildConfig.SDK_VERSION}")
                }
                is ApiContract.InAppMessages,
                is ApiContract.AppInbox,
                is ApiContract.Recommendation -> {
                    setRequestProperty(HEADER_KEY, restConfig.accessKeyProvider())
                    setRequestProperty(HEADER_VERSION, "$platform ${BuildConfig.SDK_VERSION}")
                    setRequestProperty(HEADER_DEVICE_ID, restConfig.deviceId.id)
                }
                else -> { /* NO-OP */ }
            }
            setRequestProperty(HEADER_ACCEPT, HEADER_ACCEPT_VALUE)
            setRequestProperty(HEADER_CONTENT, HEADER_CONTENT_VALUE)

            if (useSsl) {
                val socketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
                (urlConnection as HttpsURLConnection).sslSocketFactory =
                    socketFactory
            }
            if (method != HttpMethod.GET) {
                doInput = true
                setRequestProperty(HEADER_ENCODING, HEADER_ENCODING_VALUE)
                setChunkedStreamingMode(0)
            }
            if (!headers.isNullOrEmpty()) {
                headers.forEach { (key, value) ->
                    setRequestProperty(key, value)
                }
            }

            readTimeout = READ_TIMEOUT
            connectTimeout = TIMEOUT
            requestMethod = method.httpMethodName

            useCaches = false
            instanceFollowRedirects = true
        }

        return urlConnection
    }

    private fun generateUrl(url: String, params: Map<String, String?>?): String {
        return if (!params.isNullOrEmpty()) {
            attachGetParams(url, params)
        } else {
            url
        }
    }

    private fun attachBody(urlConnection: HttpURLConnection, body: String) {
        val contentEncoding =
            urlConnection.getRequestProperty(HEADER_CONTENT_ENCODING).orEmpty()
        val os = if (contentEncoding.contains(HEADER_ENCODING_VALUE)) {
            GZIPOutputStream(urlConnection.outputStream)
        } else {
            urlConnection.outputStream
        }
        val writer = BufferedWriter(OutputStreamWriter(os))

        writer.write(body)
        writer.close()
        os.close()
    }

    private fun attachGetParams(url: String, params: Map<String, String?>): String {
        val builder = Uri.parse(url).buildUpon()
        params
            .filter { it.value != null }
            .forEach { (key, value) ->
            builder.appendQueryParameter(key, value)
        }
        return builder.build().toString()
    }

}