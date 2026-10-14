package com.reteno.core.domain.controller

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUser
import com.reteno.core.data.local.model.BooleanDb
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutParams
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutParams.Position
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutType
import com.reteno.core.data.repository.IamRepository
import com.reteno.core.domain.ResultDomain
import com.reteno.core.features.iam.IamJsEvent
import com.reteno.core.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID

internal class IamControllerImpl(
    private val iamRepository: IamRepository,
    private val userDatabase: RetenoDatabaseManagerUser,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : IamController {

    private val _fullHtmlStateFlow: MutableStateFlow<ResultDomain<IamFetchResult>> =
        MutableStateFlow(ResultDomain.Idle)
    override val fullHtmlStateFlow: StateFlow<ResultDomain<IamFetchResult>> = _fullHtmlStateFlow

    private var htmlJob: Job? = null

    private val _inAppMessage = Channel<InAppMessage>(
        capacity = 2,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val inAppMessagesFlow: Flow<InAppMessage>
        get() = _inAppMessage.receiveAsFlow()

    override fun fetchIamFullHtml(interactionId: String) {
        /*@formatter:off*/ Logger.i(TAG, "fetchIamFullHtml(): ", "widgetId = [", interactionId, "]")
        /*@formatter:on*/
        _fullHtmlStateFlow.value = ResultDomain.Loading

        htmlJob = scope.launch {
            try {
                withTimeout(TIMEOUT) {
                    val baseHtml = async { iamRepository.getLocalBaseHtml() }
                    val widget = async { iamRepository.getWidgetRemote(interactionId) }

                    val base = baseHtml.await()
                    val widgetModel = widget.await()

                    val fullHtml = base.run {
                        var text = this
                        widgetModel.model?.let {
                            text = text.replace(KEY_DOCUMENT_MODEL, it.toString())
                        }
                        widgetModel.personalization?.let {
                            text = text.replace(KEY_PERSONALISATION, it.toString())
                        }
                        text
                    }
                    _fullHtmlStateFlow.value = ResultDomain.Success(
                        IamFetchResult(
                            id = UUID.randomUUID().toString(),
                            fullHtml = fullHtml,
                            layoutType = widgetModel.layoutType ?: InAppLayoutType.FULL,
                            layoutParams = widgetModel.layoutParams
                                ?: InAppLayoutParams(Position.TOP)
                        )
                    )
                }
            } catch (e: TimeoutCancellationException) {
                _fullHtmlStateFlow.value =
                    ResultDomain.Error("fetchIamFullHtml(): widgetId = [${interactionId}] TIMEOUT")
            }
        }
    }

    override fun fetchIamFullHtml(messageContent: InAppMessageContent?) {
        /*@formatter:off*/ Logger.i(TAG, "fetchIamFullHtml(): ", "widgetId = [", messageContent?.messageInstanceId, "]")
        /*@formatter:on*/
        _fullHtmlStateFlow.value = ResultDomain.Loading

        scope.launch {
            try {
                withTimeout(TIMEOUT) {
                    val baseHtml = iamRepository.getLocalBaseHtml()
                    var widgetModel = messageContent?.model.toString()
                    if (widgetModel.contains(Regex("\\$\\{.+\\}"))) {
                        val users = userDatabase.getUsers()
                        val latestPushed = users.firstOrNull { it.isSynchronizedWithBackend == BooleanDb.TRUE }
                        val newestUpdate = users
                            .filter { it.isSynchronizedWithBackend == BooleanDb.FALSE }
                            .maxByOrNull { it.createdAt }
                        widgetModel = widgetModel
                            .replace("\${firstName}", newestUpdate?.userAttributes?.firstName ?: latestPushed?.userAttributes?.firstName.orEmpty())
                            .replace("\${lastName}", newestUpdate?.userAttributes?.lastName ?: latestPushed?.userAttributes?.lastName.orEmpty())
                            .replace("\${email}", newestUpdate?.userAttributes?.email ?: latestPushed?.userAttributes?.email.orEmpty())
                            .replace("\${phone}", newestUpdate?.userAttributes?.phone ?: latestPushed?.userAttributes?.phone.orEmpty())
                            .replace("\${languageCode}", newestUpdate?.userAttributes?.languageCode ?: latestPushed?.userAttributes?.languageCode.orEmpty())
                            .replace("\${timeZone}", newestUpdate?.userAttributes?.timeZone ?: latestPushed?.userAttributes?.timeZone.orEmpty())
                            .replace("\${address}", newestUpdate?.userAttributes?.address?.address ?: latestPushed?.userAttributes?.address?.address.orEmpty())
                            .replace("\${marketId}", newestUpdate?.userAttributes?.marketId ?: latestPushed?.userAttributes?.marketId.orEmpty())
                    }
                    var text = baseHtml
                    text = text.replace(KEY_DOCUMENT_MODEL, widgetModel)
                    text = text.replace(KEY_PERSONALISATION, "{}")

                    _fullHtmlStateFlow.value = ResultDomain.Success(
                        IamFetchResult(
                            id = UUID.randomUUID().toString(),
                            fullHtml = text,
                            layoutType = messageContent?.layoutType ?: InAppLayoutType.FULL,
                            layoutParams = messageContent?.layoutParams ?: InAppLayoutParams(
                                Position.TOP
                            )
                        )
                    )
                }
            } catch (e: TimeoutCancellationException) {
                _fullHtmlStateFlow.value =
                    ResultDomain.Error("fetchIamFullHtml(): widgetId = [${messageContent?.messageInstanceId}] TIMEOUT")
            }
        }
    }

    override fun widgetInitFailed(tenantId: String, jsEvent: IamJsEvent) {
        /*@formatter:off*/ Logger.i(TAG, "widgetInitFailed(): ", "widgetId = [", tenantId, "], jsEvent = [", jsEvent, "]")
        /*@formatter:on*/
        iamRepository.widgetInitFailed(tenantId, jsEvent)
    }

    override fun reset() {
        _fullHtmlStateFlow.value = ResultDomain.Idle
        htmlJob?.cancel()
        htmlJob = null
    }

    override fun show(message: InAppMessage) {
        if (!canShowInApp()) return
        scope.launch { _inAppMessage.send(message) }
    }

    private fun canShowInApp(): Boolean {
        return _fullHtmlStateFlow.value == ResultDomain.Idle
    }


    companion object {
        private val TAG: String = IamControllerImpl::class.java.simpleName

        internal const val TIMEOUT = 30_000L
        const val KEY_DOCUMENT_MODEL = "\${documentModel}"
        const val KEY_PERSONALISATION = "\${personalisation}"
    }
}