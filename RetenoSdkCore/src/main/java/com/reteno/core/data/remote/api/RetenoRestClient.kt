package com.reteno.core.data.remote.api

import android.net.Uri
import com.reteno.core.BuildConfig
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.util.Logger
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.util.zip.GZIPOutputStream
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

internal object RetenoRestClient {

    private val TAG = RetenoRestClient::class.java.simpleName
    private val IS_DEBUG_MODE = BuildConfig.DEBUG

    private const val TIMEOUT = 10_000
    private const val READ_TIMEOUT = 15_000

    private const val HEADER_DEBUG = "X-Reteno-Debug"

    private const val HEADER_KEY = "X-Reteno-Access-Key"
    private const val HEADER_VERSION = "X-Reteno-SDK-Version"

    private const val HEADER_CONTENT = "Content-Type"
    private const val HEADER_CONTENT_VALUE = "application/json; charset=UTF-8"
    private const val HEADER_ENCODING = "Accept-Encoding"
    private const val HEADER_CONTENT_ENCODING = "Content-Encoding"
    private const val HEADER_ENCODING_VALUE = "gzip"
    private const val HEADER_ACCEPT = "Accept"
    private const val HEADER_ACCEPT_VALUE = "*/*"


    /**
     *  Perform Http request with
     *
     *  @param method - HttpMethod type
     *  @param url - base url + endpoint wrapped in ApiContract
     *  @param body - JSON body for POST and PUT methods
     *  @param queryParams - params for GET method
     *  @param responseCallback -
     *
     *  @see com.reteno.data.remote.api.HttpMethod
     *  @see com.reteno.data.remote.api.ApiContract
     *  @see com.reteno.domain.ResponseCallback
     */
    @JvmStatic
    fun makeRequest(
        method: HttpMethod,
        url: ApiContract,
        body: String?,
        queryParams: Map<String, Any>? = null,
        responseCallback: ResponseCallback
    ) {
        var urlConnection: HttpURLConnection? = null

        try {
            val urlWithParams = generateUrl(url.url, queryParams)
            val needAuthorizationHeaders = url is ApiContract.MobileApi
            /*@formatter:off*/ Logger.i(TAG, "makeRequest(): ", "method = [", method, "], url = [", url, "], body = [", body, "], queryParams = [", queryParams, "], responseCallback = [", responseCallback, "]")
            /*@formatter:on*/
            urlConnection =
                defaultHttpConnection(method, urlWithParams, needAuthorizationHeaders)

            if (body != null) {
                attachBody(urlConnection, body)
            }
            urlConnection.connect()
            Logger.i(TAG, "makeRequest(): ", "connect, headers: ${urlConnection.headerFields}")

            val responseCode = urlConnection.responseCode
            Logger.i(TAG, "makeRequest(): ", "responseCode: ", responseCode)

            when (responseCode) {
                200 -> {
                    val response = urlConnection.inputStream.bufferedReader().use { it.readText() }
                    Logger.i(TAG, "makeRequest(): ", "response: ", response)
                    responseCallback.onSuccess(response)
                }
                301 -> {
                    val response = urlConnection.inputStream.bufferedReader().use { it.readText() }
                    Logger.i(TAG, "makeRequest(): ", "response: ", response)
                    responseCallback.onSuccess(response)
                }
                else -> {
                    val response =
                        urlConnection.errorStream.bufferedReader().use { it.readText() }
                    Logger.i(TAG, "makeRequest(): ", "response: ", response)
                    responseCallback.onFailure(responseCode, response, null)
                }
            }

        } catch (e: Exception) {
            val errorMessages = """
                    m: $method,
                    u: $url,
                    e: ${e.message}
                """.trimIndent()
            Logger.d(TAG, "makeRequest(): ", errorMessages)
            responseCallback.onFailure(null, null, e)
        } finally {
            Logger.i(TAG, "makeRequest(): ", "$method $url disconnected")
            urlConnection?.disconnect()
        }
    }

    private fun defaultHttpConnection(
        method: HttpMethod,
        url: String,
        needAuthorizationHeaders: Boolean
    ): HttpURLConnection {
        val useSsl = url.startsWith("https") // TODO (bs) need to test
        val urlConnection = ConnectionManager.openConnection(url)

        urlConnection.apply {

            if (IS_DEBUG_MODE) {
                setRequestProperty(HEADER_DEBUG, "true")
            }

            if (needAuthorizationHeaders) {
                setRequestProperty(HEADER_KEY, BuildConfig.API_ACCESS_KEY)
                setRequestProperty(HEADER_VERSION, BuildConfig.SDK_VERSION.toString())
            }
            setRequestProperty(HEADER_ACCEPT, HEADER_ACCEPT_VALUE)

            if (useSsl) {
                val socketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
                (urlConnection as HttpsURLConnection).sslSocketFactory =
                    socketFactory
            }
            if (method != HttpMethod.GET) {
                doInput = true
                setRequestProperty(HEADER_CONTENT, HEADER_CONTENT_VALUE)
                setRequestProperty(HEADER_ENCODING, HEADER_ENCODING_VALUE)
                setChunkedStreamingMode(0)
            }

            readTimeout = READ_TIMEOUT
            connectTimeout = TIMEOUT
            requestMethod = method.httpMethodName

            useCaches = false
            instanceFollowRedirects = true
        }

        return urlConnection
    }

    private fun generateUrl(url: String, params: Map<String, Any>?): String {
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

    private fun attachGetParams(url: String, params: Map<String, Any>): String {
        val builder = Uri.parse(url).buildUpon()
        params.forEach { (key, value) ->
            builder.appendQueryParameter(key, value.toString())
        }
        return builder.build().toString()
    }

}