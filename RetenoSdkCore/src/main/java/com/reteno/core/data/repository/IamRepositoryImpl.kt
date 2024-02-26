package com.reteno.core.data.repository

import com.google.gson.JsonObject
import com.reteno.core.R
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.RestClientImpl.Companion.HEADER_X_AMZ_META_VERSION
import com.reteno.core.data.remote.mapper.fromJson
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.model.iam.initfailed.Data
import com.reteno.core.data.remote.model.iam.initfailed.IamJsWidgetInitiFailed
import com.reteno.core.data.remote.model.iam.initfailed.Payload
import com.reteno.core.data.remote.model.iam.message.InAppMessageResponse
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent
import com.reteno.core.data.remote.model.iam.message.InAppMessagesContentRequest
import com.reteno.core.data.remote.model.iam.message.InAppMessagesContentResponse
import com.reteno.core.data.remote.model.iam.message.InAppMessageListResponse
import com.reteno.core.data.remote.model.iam.widget.WidgetModel
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.features.iam.IamJsEvent
import com.reteno.core.util.Logger
import com.reteno.core.util.Util
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

internal class IamRepositoryImpl(
    private val apiClient: ApiClient,
    private val sharedPrefsManager: SharedPrefsManager,
    private val coroutineDispatcher: CoroutineDispatcher,
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
        return withContext(coroutineDispatcher) {
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
        return withContext(coroutineDispatcher) {
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

    override suspend fun getWidgetRemote(interactionId: String): WidgetModel {
        /*@formatter:off*/ Logger.i(TAG, "getWidget(): ", "widgetId = [", interactionId, "]")
        /*@formatter:on*/
        return withContext(coroutineDispatcher) {
            suspendCancellableCoroutine { continuation ->
                apiClient.get(
                    url = ApiContract.InAppMessages.GetInnAppWidgetByInteractionId(interactionId),
                    queryParams = null,
                    responseHandler = object : ResponseCallback {
                        override fun onSuccess(response: String) {
                            /*@formatter:off*/ Logger.i(TAG, "getWidgetRemote(): onSuccess(): ", "response = [", response, "]")
                            /*@formatter:on*/
                            continuation.resume(
                                response.fromJson<JsonObject>().run {
                                    WidgetModel(
                                        get("model").toString(),
                                        get("personalisation").toString(),
                                    )
                                }
                            )
                        }

                        override fun onFailure(
                            statusCode: Int?,
                            response: String?,
                            throwable: Throwable?
                        ) {
                            /*@formatter:off*/ Logger.i(TAG, "getWidgetRemote(): onFailure(): ", "statusCode = [", statusCode, "], response = [", response, "], throwable = [", throwable, "]")
                            /*@formatter:on*/
                            continuation.resume(WidgetModel(WIDGET))
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
        val initFailed = IamJsWidgetInitiFailed(tenantId = widgetId, data = data.toJson())
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

    override suspend fun getInAppMessages(): List<InAppMessageResponse> {
        /*@formatter:off*/ Logger.i(TAG, "getInAppMessages(): ", "")
        /*@formatter:on*/
        return withContext(coroutineDispatcher) {
            suspendCancellableCoroutine { continuation ->
                apiClient.get(
                    url = ApiContract.InAppMessages.GetInAppMessages,
                    queryParams = null,
                    responseHandler = object : ResponseCallback {
                        override fun onSuccess(response: String) {
                            /*@formatter:off*/ Logger.i(TAG, "getInAppMessages(): onSuccess(): ", "response = [", response, "]")
                            /*@formatter:on*/
                            continuation.resume(
                                response.fromJson<InAppMessageListResponse>().messages
                            )
                        }

                        override fun onFailure(
                            statusCode: Int?,
                            response: String?,
                            throwable: Throwable?
                        ) {
                            /*@formatter:off*/ Logger.i(TAG, "getInAppMessages(): onFailure(): ", "statusCode = [", statusCode, "], response = [", response, "], throwable = [", throwable, "]")
                            /*@formatter:on*/
                            continuation.resume(emptyList())
//                            continuation.resumeWithException()
                        }
                    }
                )
            }
        }
    }

    override suspend fun getInAppMessagesContent(messageInstanceIds: List<Long>): List<InAppMessageContent> {
        /*@formatter:off*/ Logger.i(TAG, "getInAppMessagesContent(): ", "messageInstanceIds = [", messageInstanceIds.toString(), "]")
        /*@formatter:on*/
        return withContext(coroutineDispatcher) {
            suspendCancellableCoroutine { continuation ->
                apiClient.post(
                    url = ApiContract.InAppMessages.GetInAppMessagesContent,
                    jsonBody = InAppMessagesContentRequest(messageInstanceIds).toJson(),
                    responseHandler = object : ResponseCallback {
                        override fun onSuccess(response: String) {
                            /*@formatter:off*/ Logger.i(TAG, "getInAppMessagesContent(): onSuccess(): ", "response = [", response, "]")
                            /*@formatter:on*/
                            continuation.resume(
                                response.fromJson<InAppMessagesContentResponse>().contents
                            )
                        }

                        override fun onFailure(
                            statusCode: Int?,
                            response: String?,
                            throwable: Throwable?
                        ) {
                            /*@formatter:off*/ Logger.i(TAG, "getInAppMessagesContent(): onFailure(): ", "statusCode = [", statusCode, "], response = [", response, "], throwable = [", throwable, "]")
                            /*@formatter:on*/
                            continuation.resume(emptyList())
//                            continuation.resumeWithException()
                        }
                    }
                )
            }
        }
    }

    companion object {
        private val TAG: String = IamRepositoryImpl::class.java.simpleName

        private const val BASE_HTML_VERSION_DEFAULT = "0"
        private val WIDGET = Util.readFromRaw(R.raw.widget) ?: ""
    }
}