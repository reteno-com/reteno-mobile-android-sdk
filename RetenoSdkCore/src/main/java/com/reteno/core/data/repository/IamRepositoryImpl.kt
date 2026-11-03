package com.reteno.core.data.repository

import android.content.Context
import java.time.Instant
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppInteraction
import com.reteno.core.data.remote.model.iam.message.WorkflowMessageContentRequest
import com.reteno.core.R
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppMessages
import com.reteno.core.data.local.file.FileManager
import com.reteno.core.data.local.mappers.mapDbToInAppMessages
import com.reteno.core.data.local.mappers.mapResponseToInAppMessages
import com.reteno.core.data.local.mappers.toDB
import com.reteno.core.data.local.mappers.updateFromDb
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.RestClientImpl.Companion.HEADER_X_AMZ_META_VERSION
import com.reteno.core.data.remote.mapper.fromJson
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.model.iam.displayrules.async.AsyncRulesCheckRequest
import com.reteno.core.data.remote.model.iam.displayrules.async.AsyncRulesCheckResponse
import com.reteno.core.data.remote.model.iam.displayrules.async.AsyncRulesCheckResult
import com.reteno.core.data.remote.model.iam.initfailed.Data
import com.reteno.core.data.remote.model.iam.initfailed.IamJsWidgetInitiFailed
import com.reteno.core.data.remote.model.iam.initfailed.Payload
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent
import com.reteno.core.data.remote.model.iam.message.InAppMessageListResponse
import com.reteno.core.data.remote.model.iam.message.InAppMessagesContentRequest
import com.reteno.core.data.remote.model.iam.message.InAppMessagesContentResponse
import com.reteno.core.data.remote.model.iam.message.InAppMessagesList
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
    private val sharedPrefsManager: SharedPrefsManager,
    private val databaseManager: RetenoDatabaseManagerInAppMessages,
    private val coroutineDispatcher: CoroutineDispatcher,
    private val inAppInteractionDatabase: RetenoDatabaseManagerInAppInteraction? = null
) : IamRepository {
    private var listContext: String? = null

    override fun isWorkflowContextCurrent(message: InAppMessage): Boolean =
        message.workflowContext == apiClient.workflowContext()

    override fun isWorkflowEligible(message: InAppMessage): Boolean =
        message.inWorkflow && !message.iid.isNullOrBlank() &&
            isWorkflowContextCurrent(message) &&
            runCatching { Instant.now().isBefore(Instant.parse(message.expiresAt)) }.getOrDefault(false) &&
            inAppInteractionDatabase?.isWorkflowShown(requireNotNull(message.workflowContext), requireNotNull(message.iid)) == false

    override suspend fun getWorkflowContent(messages: List<InAppMessage>): List<InAppMessageContent> {
        val eligible = messages.filter(::isWorkflowEligible)
        return eligible.chunked(100).flatMap { batch ->
            val owner = requireNotNull(batch.first().workflowContext)
            withContext(coroutineDispatcher) {
                suspendCancellableCoroutine { continuation ->
                    apiClient.postInWorkflowContext(
                        owner, ApiContract.InAppMessages.GetInAppMessagesContent,
                        InAppMessagesContentRequest(messages = batch.map {
                            WorkflowMessageContentRequest(requireNotNull(it.iid), it.messageInstanceId)
                        }).toJson(),
                        object : ResponseCallback {
                            override fun onSuccess(response: String) {
                                val contents = runCatching { response.fromJson<InAppMessagesContentResponse>().contents }
                                    .getOrDefault(emptyList())
                                continuation.resume(contents.filter { content ->
                                    batch.any { it.iid == content.iid && it.messageInstanceId == content.messageInstanceId && isWorkflowEligible(it) }
                                })
                            }
                            override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                                Logger.w(TAG, "Workflow content unavailable; presentation deferred. statusCode=", statusCode)
                                continuation.resume(emptyList())
                            }
                        }
                    )
                }
            }
        }
    }

    override suspend fun getLocalBaseHtml(): String {
        return fileManager.getBaseHtmlContent()
    }

    override suspend fun getBaseHtml(): String {
        /*@formatter:off*/ Logger.i(TAG, "fetchBaseHtml(): ", "") 
        /*@formatter:on*/
        val versionRemote = getBaseHtmlVersionRemote() ?: BASE_HTML_VERSION_DEFAULT
        val versionLocal = sharedPrefsManager.getIamBaseHtmlVersion() ?: BASE_HTML_VERSION_DEFAULT
        val cachedDeprecatedHtml = sharedPrefsManager.getIamBaseHtmlContent()
        if (cachedDeprecatedHtml.isNotBlank()) {
            //Migration for moving base inapp content to file from sharedprefs
            fileManager.saveBaseHtmlContent(cachedDeprecatedHtml)
            sharedPrefsManager.clearBaseHtmlContent()
        }
        val baseHtmlContent = if (versionRemote == versionLocal) {
            fileManager.getBaseHtmlContent()
        } else {
            val baseHtmlContentRemote =
                getBaseHtmlContentRemote() ?: fileManager.getBaseHtmlContent()

            sharedPrefsManager.saveIamBaseHtmlVersion(versionRemote)
            fileManager.saveBaseHtmlContent(baseHtmlContentRemote)
            sharedPrefsManager.clearBaseHtmlContent()
            baseHtmlContentRemote
        }

        return baseHtmlContent
    }

    private suspend fun getBaseHtmlVersionRemote(): String? {
        /*@formatter:off*/ Logger.i(TAG, "getBaseHtmlVersionRemote(): ", "")
        /*@formatter:on*/
        val iamUrl = sharedPrefsManager.getIamBaseUrl()
        val url = if (iamUrl != null) {
            ApiContract.Custom(url = iamUrl)
        } else {
            ApiContract.InAppMessages.BaseHtml
        }
        return withContext(coroutineDispatcher) {
            suspendCancellableCoroutine { continuation ->
                apiClient.head(url = url,
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
                            Logger.d(TAG, "getBaseHtmlVersionRemote(): onFailure(): ")
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
        val iamUrl = sharedPrefsManager.getIamBaseUrl()
        val url = if (iamUrl != null) {
            ApiContract.Custom(url = iamUrl)
        } else {
            ApiContract.InAppMessages.BaseHtml
        }
        return withContext(coroutineDispatcher) {
            suspendCancellableCoroutine { continuation ->
                apiClient.get(url = url,
                    queryParams = null,
                    responseHandler = object : ResponseCallback {
                        override fun onSuccess(response: String) {
                            Logger.d(TAG, "getBaseHtmlContentRemote(): onSuccess(): ")
                            /*@formatter:on*/
                            continuation.resume(response)
                        }

                        override fun onFailure(
                            statusCode: Int?,
                            response: String?,
                            throwable: Throwable?
                        ) {
                            Logger.d(TAG, "getBaseHtmlContentRemote(): onFailure(): ")
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
                            Logger.d(TAG, "getWidgetRemote(): onSuccess(): ")
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
                            Logger.d(TAG, "getWidgetRemote(): onFailure(): ")
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
        Logger.d(TAG, "widgetInitFailed(): ")
        /*@formatter:on*/

        val payload = Payload(reason = jsEvent.payload?.reason)
        val data = Data(type = jsEvent.type.name, payload = payload)
        val initFailed = IamJsWidgetInitiFailed(tenantId = widgetId, data = data.toJson())
        apiClient.post(
            ApiContract.InAppMessages.WidgetInitFailed,
            initFailed.toJson(),
            object : ResponseCallback {
                override fun onSuccess(response: String) {
                    Logger.d(TAG, "widgetInitFailed(): onSuccess(): ")
                    /*@formatter:on*/
                }

                override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                    Logger.d(TAG, "widgetInitFailed(): onFailure(): ")
                    /*@formatter:on*/
                }
            })
    }

    override suspend fun getInAppMessages(): InAppMessagesList {
        /*@formatter:off*/ Logger.i(TAG, "getInAppMessages(): ", "")
        /*@formatter:on*/
        return withContext(coroutineDispatcher) {
            val owner = apiClient.workflowContext()
            val etag = sharedPrefsManager.getIamEtag()
            val headersWithEtag: Map<String, String>? = if (etag != null && listContext == owner) {
                mapOf(HEADER_ETAG_REQUEST to etag)
            } else {
                null
            }

            suspendCancellableCoroutine { continuation ->
                apiClient.get(
                    url = ApiContract.InAppMessages.GetInAppMessages,
                    headers = headersWithEtag,
                    queryParams = mapOf("includeWorkflow" to "true"),
                    responseHandler = object : ResponseCallback {
                        override fun onSuccess(
                            headers: Map<String, List<String>>,
                            response: String
                        ) {
                            Logger.d(TAG, "getInAppMessages(): onSuccess(): ")
                            /*@formatter:on*/
                            if (owner != apiClient.workflowContext()) {
                                continuation.resume(InAppMessagesList(isSuccessful = false))
                                return
                            }
                            val localMessages = databaseManager.getInAppMessages()
                            val remoteMessagesResponse =
                                response.fromJson<InAppMessageListResponse>().messages
                            var remoteMessages: List<InAppMessage> = emptyList()
                            try {
                                remoteMessages =
                                    remoteMessagesResponse.mapResponseToInAppMessages().mapNotNull { message ->
                                        if (!message.inWorkflow) message else {
                                            val previous = localMessages.firstOrNull { it.iid == message.iid && it.workflowContext == owner }
                                            when {
                                                previous == null -> message
                                                previous.messageInstanceId != message.messageInstanceId -> null
                                                else -> message.copy(expiresAt = listOfNotNull(previous.expiresAt, message.expiresAt)
                                                    .minByOrNull { Instant.parse(it) })
                                            }
                                        }
                                    }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            remoteMessages.filter { it.inWorkflow }.forEach { it.workflowContext = owner }
                            listContext = owner
                            remoteMessages.filterNot { it.inWorkflow }.forEach { remoteMessage ->
                                val localMessage = localMessages.firstOrNull {
                                    it.iid == null && it.messageId == remoteMessage.messageId
                                }
                                localMessage?.let {
                                    remoteMessage.updateFromDb(it)
                                }
                            }
                            continuation.resume(
                                InAppMessagesList(
                                    messages = remoteMessages,
                                    etag = headers[HEADER_ETAG_RESPONSE]?.firstOrNull(),
                                    isFromRemote = true
                                )
                            )
                        }

                        override fun onSuccess(response: String) {
                            onSuccess(emptyMap(), response)
                        }

                        override fun onFailure(
                            statusCode: Int?,
                            response: String?,
                            throwable: Throwable?
                        ) {
                            Logger.d(TAG, "getInAppMessages(): onFailure(): ")
                            /*@formatter:on*/
                            if (statusCode == IN_APP_NO_CHANGES_CODE) {
                                val localMessages = databaseManager.getInAppMessages()
                                continuation.resume(
                                    InAppMessagesList(
                                        messages = if (owner == apiClient.workflowContext()) localMessages
                                            .filter { it.iid == null || it.workflowContext == owner }.mapDbToInAppMessages() else emptyList(),
                                        isFromRemote = false,
                                        isSuccessful = owner == apiClient.workflowContext()
                                    )
                                )
                            } else {
                                continuation.resume(InAppMessagesList(isSuccessful = false))
                            }
                        }
                    }
                )
            }
        }
    }

    override suspend fun getInAppMessagesContent(messageInstanceIds: List<Long>): List<InAppMessageContent> {
        /*@formatter:off*/ Logger.i(TAG, "getInAppMessagesContent(): ", "messageInstanceIds = [", messageInstanceIds.toString(), "]")
        /*@formatter:on*/
        if (messageInstanceIds.isEmpty()) return emptyList()

        return withContext(coroutineDispatcher) {
            suspendCancellableCoroutine { continuation ->
                apiClient.postWithRetry(
                    url = ApiContract.InAppMessages.GetInAppMessagesContent,
                    jsonBody = InAppMessagesContentRequest(messageInstanceIds).toJson(),
                    retryCount = 3,
                    responseHandler = object : ResponseCallback {
                        override fun onSuccess(response: String) {
                            Logger.d(TAG, "getInAppMessagesContent(): onSuccess(): ")
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
                            Logger.d(TAG, "getInAppMessagesContent(): onFailure(): ")
                            /*@formatter:on*/
                            continuation.resume(emptyList())
                        }
                    }
                )
            }
        }
    }

    override suspend fun saveInAppMessages(inAppMessageList: InAppMessagesList) {
        if (inAppMessageList.isFromRemote) {
            Logger.d(TAG, "saveInAppMessages(): ")
            /*@formatter:on*/
            val owner = apiClient.workflowContext()
            val cached = databaseManager.getInAppMessages()
            if (cached.none { it.iid != null }) databaseManager.deleteAllInAppMessages()
            else databaseManager.deleteInAppMessages(cached.filter { it.iid == null || it.workflowContext == owner })
            databaseManager.insertInAppMessages(inAppMessageList.messages.filter { !it.inWorkflow || it.workflowContext == owner }.map { it.toDB() })
            sharedPrefsManager.saveIamEtag(inAppMessageList.etag)
        }
    }

    override suspend fun updateInAppMessages(inAppMessages: List<InAppMessage>) {
        Logger.d(TAG, "updateInAppMessages(): ")
        /*@formatter:on*/
        val messages = inAppMessages.filter { !it.inWorkflow || it.workflowContext == apiClient.workflowContext() }.map { it.toDB() }
        databaseManager.deleteInAppMessages(messages)
        databaseManager.insertInAppMessages(messages)
    }

    override suspend fun checkUserInSegments(segmentIds: List<Long>): List<AsyncRulesCheckResult> {
        /*@formatter:off*/ Logger.i(TAG, "checkUserInSegments(): ", "segmentIds = [", segmentIds.toString(), "]")
        /*@formatter:on*/
        if (segmentIds.isEmpty()) return emptyList()

        return withContext(coroutineDispatcher) {
            suspendCancellableCoroutine { continuation ->
                apiClient.post(
                    url = ApiContract.InAppMessages.CheckUserInSegments,
                    jsonBody = AsyncRulesCheckRequest.createSegmentRequest(segmentIds).toJson(),
                    responseHandler = object : ResponseCallback {
                        override fun onSuccess(response: String) {
                            Logger.d(TAG, "checkUserInSegments(): onSuccess(): ")
                            /*@formatter:on*/
                            continuation.resume(
                                response.fromJson<AsyncRulesCheckResponse>().checks
                            )
                        }

                        override fun onFailure(
                            statusCode: Int?,
                            response: String?,
                            throwable: Throwable?
                        ) {
                            Logger.d(TAG, "checkUserInSegments(): onFailure(): ")
                            /*@formatter:on*/
                            continuation.resume(emptyList())
                        }
                    }
                )
            }
        }
    }

    companion object {
        private val TAG: String = IamRepositoryImpl::class.java.simpleName

        private const val BASE_HTML_VERSION_DEFAULT = "20250307-1527-f34d4c8"
        private const val IN_APP_NO_CHANGES_CODE = 304
        private const val HEADER_ETAG_RESPONSE = "ETag"
        private const val HEADER_ETAG_REQUEST = "If-None-Match"
    }
}
