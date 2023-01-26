package com.reteno.core.data.repository

import com.reteno.core.R
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.RestClientImpl.Companion.HEADER_X_AMZ_META_VERSION
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.model.iam.initfailed.Data
import com.reteno.core.data.remote.model.iam.initfailed.IamJsWidgetInitiFailed
import com.reteno.core.data.remote.model.iam.initfailed.Payload
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.features.iam.IamJsEvent
import com.reteno.core.util.Logger
import com.reteno.core.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

internal class IamRepositoryImpl(
    private val apiClient: ApiClient,
    private val sharedPrefsManager: SharedPrefsManager
) : IamRepository {

    override suspend fun getBaseHtml(): String {
        /*@formatter:off*/ Logger.i(TAG, "fetchBaseHtml(): ", "") 
        /*@formatter:on*/
        val versionRemote = getBaseHtmlVersionRemote() ?: BASE_HTML_VERSION_DEFAULT
        val versionLocal = sharedPrefsManager.getIamBaseHtmlVersion() ?: BASE_HTML_VERSION_DEFAULT

        val baseHtmlContent = if (versionRemote == versionLocal) {
            sharedPrefsManager.getIamBaseHtmlContent()
        } else {
            val baseHtmlContentRemote =
                getBaseHtmlContentRemote() ?: sharedPrefsManager.getIamBaseHtmlContent()

            sharedPrefsManager.saveIamBaseHtmlVersion(versionRemote)
            sharedPrefsManager.saveIamBaseHtmlContent(baseHtmlContentRemote)
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

    override fun widgetInitFailed(widgetId: String, jsEvent: IamJsEvent) {
        /*@formatter:off*/ Logger.i(TAG, "widgetInitFailed(): ", "widgetId = [", widgetId, "], jsEvent = [", jsEvent, "]")
        /*@formatter:on*/

        val payload = Payload(reason = jsEvent.payload?.reason)
        val data = Data(type = jsEvent.type.name, payload = payload)
        val initFailed = IamJsWidgetInitiFailed(tenantId = widgetId, data = data)
        apiClient.post(
            ApiContract.InAppMessages.WidgetInitFailed,
            initFailed.toJson(),
            object : ResponseCallback {
                override fun onSuccess(response: String) {
                    /*@formatter:off*/ Logger.i(TAG, "widgetInitFailed(): onSuccess(): ", "widgetId = [", widgetId, "], jsEvent = [", jsEvent, "], response = [", response, "]")
                    /*@formatter:on*/
                }

                override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                    /*@formatter:off*/ Logger.i(TAG, "widgetInitFailed(): onFailure(): ", "statusCode = [", statusCode, "], response = [", response, "], throwable = [", throwable, "]")
                    /*@formatter:on*/
                }
            })
    }

    companion object {
        private val TAG: String = IamRepositoryImpl::class.java.simpleName

        private const val BASE_HTML_VERSION_DEFAULT = "0"
        private val WIDGET = Util.readFromRaw(R.raw.widget) ?: ""
    }
}