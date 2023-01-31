package com.reteno.core.data.repository

import com.reteno.core.R
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.RestClientImpl.Companion.HEADER_X_AMZ_META_VERSION
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.util.Logger
import com.reteno.core.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

internal class InAppMessagesRepositoryImpl(
    private val apiClient: ApiClient,
    private val sharedPrefsManager: SharedPrefsManager
) : InAppMessagesRepository {

    override suspend fun getBaseHtml(): String {
        /*@formatter:off*/ Logger.i(TAG, "fetchBaseHtml(): ", "") 
        /*@formatter:on*/
        val versionRemote = getBaseHtmlVersionRemote() ?: BASE_HTML_VERSION_DEFAULT
        val versionLocal = sharedPrefsManager.getInAppMessagesBaseHtmlVersion() ?: BASE_HTML_VERSION_DEFAULT

        val baseHtmlContent = if (versionRemote == versionLocal) {
            sharedPrefsManager.getInAppMessagesBaseHtmlContent()
        } else {
            val baseHtmlContentRemote =
                getBaseHtmlContentRemote() ?: sharedPrefsManager.getInAppMessagesBaseHtmlContent()

            sharedPrefsManager.saveInAppMessagesBaseHtmlVersion(versionRemote)
            sharedPrefsManager.saveInAppMessagesBaseHtmlContent(baseHtmlContentRemote)
            baseHtmlContentRemote
        }

        return baseHtmlContent
    }

    private suspend fun getBaseHtmlVersionRemote(): String? {
        /*@formatter:off*/ Logger.i(TAG, "getBaseHtmlVersionRemote(): ", "")
        /*@formatter:on*/
        return withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                apiClient.head(url = ApiContract.InAppMessages.BaseHtml,
                    queryParams = null,
                    responseHandler = object : ResponseCallback {
                        override fun onSuccess(
                            headers: Map<String, List<String>>,
                            response: String
                        ) {
                            val version = headers[HEADER_X_AMZ_META_VERSION]?.get(0)
                            /*@formatter:off*/ Logger.i(TAG, "getBaseHtmlVersionRemote(): onSuccess(): ", "version = [", version, "]")
                            /*@formatter:on*/
                            continuation.resume(version)
                        }

                        override fun onSuccess(response: String) {}
                        override fun onFailure(
                            statusCode: Int?,
                            response: String?,
                            throwable: Throwable?
                        ) {
                            /*@formatter:off*/ Logger.i(TAG, "getBaseHtmlVersionRemote(): onFailure(): ", "statusCode = [", statusCode, "], response = [", response, "], throwable = [", throwable, "]")
                            /*@formatter:on*/
                            continuation.resume(null)
                        }
                    }
                )
            }
        }
    }

    private suspend fun getBaseHtmlContentRemote(): String? {
        /*@formatter:off*/ Logger.i(TAG, "getBaseHtmlContentRemote(): ", "")
        /*@formatter:on*/
        return withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                apiClient.get(url = ApiContract.InAppMessages.BaseHtml,
                    queryParams = null,
                    responseHandler = object : ResponseCallback {
                        override fun onSuccess(response: String) {
                            /*@formatter:off*/ Logger.i(TAG, "getBaseHtmlContentRemote(): onSuccess(): ", "response = [", response, "]")
                            /*@formatter:on*/
                            continuation.resume(response)
                        }

                        override fun onFailure(
                            statusCode: Int?,
                            response: String?,
                            throwable: Throwable?
                        ) {
                            /*@formatter:off*/ Logger.i(TAG, "getBaseHtmlContentRemote(): onFailure(): ", "statusCode = [", statusCode, "], response = [", response, "], throwable = [", throwable, "]")
                            /*@formatter:on*/
                            continuation.resume(null)
                        }
                    }
                )
            }
        }
    }


    override suspend fun getWidget(widgetId: String): String {
        /*@formatter:off*/ Logger.i(TAG, "getWidget(): ", "widgetId = [", widgetId, "]")
        /*@formatter:on*/
        return withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                /*@formatter:off*/ Logger.i(TAG, "fetchInApp(): ", "continuation = [", continuation, "]")
                /*@formatter:on*/
                apiClient.get(url = ApiContract.MobileApi.InAppMessages,
                    queryParams = null,
                    responseHandler = object : ResponseCallback {
                        override fun onSuccess(response: String) {
                            // TODO: Replace with actual response
                            continuation.resume(WIDGET)
                        }

                        override fun onFailure(
                            statusCode: Int?,
                            response: String?,
                            throwable: Throwable?
                        ) {
                            // TODO: Replace with error handling
                            continuation.resume(WIDGET)
//                            continuation.resumeWithException()
                        }
                    }
                )
            }
        }
    }

    companion object {
        private val TAG: String = InAppMessagesRepositoryImpl::class.java.simpleName

        private const val BASE_HTML_VERSION_DEFAULT = "0"
        private val WIDGET = Util.readFromRaw(R.raw.widget) ?: ""
    }
}