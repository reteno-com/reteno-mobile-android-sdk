package com.reteno.core.internal.action.iam

import com.reteno.core.data.local.mappers.mapDbToInAppMessages
import com.reteno.core.data.local.mappers.mapResponseToInAppMessages
import com.reteno.core.data.local.mappers.updateFromDb
import com.reteno.core.data.local.model.iam.InAppMessageDb
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.mapper.fromJson
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.data.remote.model.iam.message.InAppMessageListResponse
import com.reteno.core.data.remote.model.iam.message.InAppMessagesContentRequest
import com.reteno.core.data.remote.model.iam.message.InAppMessagesContentResponse
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.action.trace.stepOrNull
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.iam.InAppMessagesState
import com.reteno.core.internal.network.ApiResult
import com.reteno.core.internal.network.SuspendApiClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

internal interface FetchInAppMessages {
    suspend operator fun invoke(): Result<List<InAppMessage>>
}

internal class FetchInAppMessagesAction(
    private val api: SuspendApiClient,
    private val cache: InAppMessagesCache,
    private val prefs: SharedPrefsManager,
    private val checkSegments: CheckInAppSegments,
    private val state: InAppMessagesState,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) : FetchInAppMessages {

    override suspend operator fun invoke(): Result<List<InAppMessage>> = runner("FetchInAppMessages") {
        fetch()
    }

    private suspend fun ActionTrace.fetch(): List<InAppMessage> {
        val cached = step<List<InAppMessageDb>>(
            "load cached in-app messages from local database",
            detail = { "count=${it.size}" }
        ) {
            cache.load()
        }
        val listVersion = readListVersion(cached)
        val response = step("load in-app messages from server") {
            api.request(
                HttpMethod.GET,
                ApiContract.InAppMessages.GetInAppMessages,
                headers = if (listVersion == null) null else mapOf(HEADER_IF_NONE_MATCH to listVersion)
            )
        }
        val fromServer = readServerList(response)
        val messages = if (fromServer == null) {
            useCached(cached)
        } else {
            useServerList(fromServer, cached, (response as ApiResult.Success).headers)
        }
        state.replaceMessages(messages)
        return messages
    }

    private suspend fun ActionTrace.readListVersion(cached: List<InAppMessageDb>): String? {
        if (cached.isEmpty()) return null
        return step("read in-app list version from local settings") {
            withContext(dispatchers.io) { prefs.getIamEtag() }
        }
    }

    private suspend fun ActionTrace.readServerList(response: ApiResult): List<InAppMessage>? {
        if (response is ApiResult.Failure) {
            note {
                if (response.statusCode == NOT_MODIFIED) {
                    "in-app messages unchanged on server"
                } else {
                    "in-app messages unavailable on server, using cached ones"
                }
            }
            return null
        }
        val body = (response as ApiResult.Success).body
        return stepOrNull("read in-app messages from server response", detail = { "count=${it.size}" }) {
            body.fromJson<InAppMessageListResponse>().messages.mapResponseToInAppMessages()
        }
    }

    private suspend fun ActionTrace.useServerList(
        messages: List<InAppMessage>,
        cached: List<InAppMessageDb>,
        headers: Map<String, List<String>>
    ): List<InAppMessage> {
        step("restore show history from cached in-app messages") {
            messages.forEach { message ->
                cached.firstOrNull { it.messageId == message.messageId }?.let(message::updateFromDb)
            }
        }
        loadContentsAndCheckSegments(messages)
        step("save in-app messages to local database") {
            cache.replaceAll(messages)
        }
        step("save in-app list version to local settings") {
            withContext(dispatchers.io) { prefs.saveIamEtag(headers[HEADER_ETAG]?.firstOrNull()) }
        }
        return messages
    }

    private suspend fun ActionTrace.useCached(cached: List<InAppMessageDb>): List<InAppMessage> {
        val messages = step<List<InAppMessage>>("read cached in-app messages", detail = { "count=${it.size}" }) {
            cached.mapDbToInAppMessages()
        }
        loadContentsAndCheckSegments(messages)
        step("save refreshed cached in-app messages to local database") {
            cache.replaceAll(messages)
        }
        return messages
    }

    private suspend fun ActionTrace.loadContentsAndCheckSegments(messages: List<InAppMessage>) = coroutineScope {
        val contents = async { loadMissingContents(messages) }
        checkSegments(messages, persist = false)
        contents.await()
    }

    private suspend fun ActionTrace.loadMissingContents(messages: List<InAppMessage>) {
        val withoutContent = messages.filter { it.content == null }
        if (withoutContent.isEmpty()) return
        val request = InAppMessagesContentRequest(withoutContent.map { it.messageInstanceId })
        val response = step("load in-app contents from server") {
            api.request(
                HttpMethod.POST,
                ApiContract.InAppMessages.GetInAppMessagesContent,
                request.toJson(),
                retryCount = CONTENT_RETRIES
            )
        }
        if (response is ApiResult.Failure) return
        val body = (response as ApiResult.Success).body
        val contents = stepOrNull("read in-app contents from server response") {
            body.fromJson<InAppMessagesContentResponse>().contents
        } ?: return
        withoutContent.forEach { message ->
            message.content = contents.firstOrNull { it.messageInstanceId == message.messageInstanceId }
        }
    }

    private companion object {
        const val NOT_MODIFIED = 304
        const val CONTENT_RETRIES = 3
        const val HEADER_ETAG = "ETag"
        const val HEADER_IF_NONE_MATCH = "If-None-Match"
    }
}
