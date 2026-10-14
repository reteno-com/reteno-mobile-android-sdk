package com.reteno.core.data.repository

import android.content.Context
import com.reteno.core.R
import com.reteno.core.data.local.file.FileManager
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.mapper.fromJson
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.model.iam.initfailed.Data
import com.reteno.core.data.remote.model.iam.initfailed.IamJsWidgetInitiFailed
import com.reteno.core.data.remote.model.iam.initfailed.Payload
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent
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
    private val context: Context,
    private val apiClient: ApiClient,
    private val fileManager: FileManager,
    private val coroutineDispatcher: CoroutineDispatcher
) : IamRepository {

    override suspend fun getLocalBaseHtml(): String {
        return fileManager.getBaseHtmlContent()
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
                                response.fromJson<WidgetModel>()
                            )
                        }

                        override fun onFailure(
                            statusCode: Int?,
                            response: String?,
                            throwable: Throwable?
                        ) {
                            /*@formatter:off*/ Logger.i(TAG, "getWidgetRemote(): onFailure(): ", "statusCode = [", statusCode, "], response = [", response, "], throwable = [", throwable, "]")
                            /*@formatter:on*/
                            continuation.resume(
                                WidgetModel(
                                    model = (Util.readFromRaw(context, R.raw.widget) ?: "{}").fromJson(),
                                    layoutParams = null,
                                    layoutType = InAppMessageContent.InAppLayoutType.FULL,
                                    personalization = null
                                )
                            )
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

    companion object {
        private val TAG: String = IamRepositoryImpl::class.java.simpleName

    }
}