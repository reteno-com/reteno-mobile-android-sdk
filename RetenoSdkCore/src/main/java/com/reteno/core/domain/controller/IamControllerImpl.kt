package com.reteno.core.domain.controller

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUser
import com.reteno.core.data.local.mappers.toDomain
import com.reteno.core.data.local.model.BooleanDb
import com.reteno.core.data.remote.model.iam.displayrules.targeting.InAppWithEvent
import com.reteno.core.data.remote.model.iam.displayrules.targeting.InAppWithTime
import com.reteno.core.data.remote.model.iam.displayrules.targeting.TargetingRule
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutParams
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutParams.Position
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutType
import com.reteno.core.data.repository.IamRepository
import com.reteno.core.domain.ResultDomain
import com.reteno.core.domain.model.event.Event
import com.reteno.core.features.iam.IamJsEvent
import com.reteno.core.features.iam.InAppPauseBehaviour
import com.reteno.core.internal.action.event.eventFlow
import com.reteno.core.internal.iam.DisplayDecision
import com.reteno.core.internal.iam.InAppDisplayRules
import com.reteno.core.internal.iam.InAppMessagesState
import com.reteno.core.lifecycle.RetenoSessionHandler
import com.reteno.core.util.Logger
import com.reteno.core.util.bufferUntil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.UUID

internal class IamControllerImpl(
    private val iamRepository: IamRepository,
    private val sessionHandler: RetenoSessionHandler,
    private val userDatabase: RetenoDatabaseManagerUser,
    private val displayRules: InAppDisplayRules,
    private val state: InAppMessagesState,
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

    private val isFirstMessagesProcessed = MutableStateFlow(false)

    init {
        eventFlow
            .bufferUntil(isFirstMessagesProcessed)
            .onEach { notifyEventOccurred(it) }
            .retry()
            .launchIn(scope)
    }

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

    override suspend fun getInAppMessages() = coroutineScope {
        try {
            val messageListModel = iamRepository.getInAppMessages()
            val inAppMessages = messageListModel.messages

            val messagesWithNoContent = inAppMessages.filter { it.content == null }
            val contentIds = messagesWithNoContent.map { it.messageInstanceId }
            val contentsDeferred = async { iamRepository.getInAppMessagesContent(contentIds) }

            updateSegmentStatuses(
                inAppMessages,
                updateCacheOnSuccess = messageListModel.isFromRemote.not()
            )

            val contents = contentsDeferred.await()
            messagesWithNoContent.forEach { message ->
                message.content = contents.firstOrNull {
                    it.messageInstanceId == message.messageInstanceId
                }
            }

            iamRepository.saveInAppMessages(messageListModel)
            sortMessages(inAppMessages)
        } catch (e: Exception) {
            Logger.e(TAG, "getInAppMessages():", e)
        }
    }

    override fun refreshSegmentation() {
        scope.launch {
            try {
                val messageListModel = iamRepository.getInAppMessages()
                val inAppMessages = messageListModel.messages

                updateSegmentStatuses(
                    inAppMessages,
                    updateCacheOnSuccess = messageListModel.isFromRemote.not()
                )
            } catch (e: Exception) {
                Logger.e(TAG, "refreshSegmentation():", e)
            }
        }
    }

    private suspend fun notifyEventOccurred(event: Event): Boolean {
        val inAppsMatchingEvent = state.waitingForEvent().filter { inapp ->
            displayRules.matchesEvent(inapp.event, event)
        }
        if (inAppsMatchingEvent.isEmpty()) return false

        return tryShowInAppFromList(inAppsMatchingEvent.map { it.inApp }.toMutableList())
    }

    override fun pauseInAppMessages(isPaused: Boolean) {
        state.setPaused(isPaused)?.let(::showInApp)
    }

    override fun updateInAppMessage(inAppMessage: InAppMessage) {
        scope.launch {
            withContext(Dispatchers.IO) {
                iamRepository.updateInAppMessages(listOf(inAppMessage))
            }
        }
    }

    override fun setPauseBehaviour(behaviour: InAppPauseBehaviour) {
        state.setPauseBehaviour(behaviour)
    }

    override fun preloadHtml() {
        scope.launch {
            iamRepository.getBaseHtml()
        }
    }

    private suspend fun sortMessages(inAppMessages: List<InAppMessage>) {
        val inAppsWithEvents = mutableListOf<InAppWithEvent>()
        val inAppsWithTimer = mutableListOf<InAppWithTime>()
        val inAppsOnAppStart = mutableListOf<InAppMessage>()

        inAppMessages.forEach { message ->
            val includeRules = message.displayRules.targeting?.include
            if (includeRules != null) {
                includeRules.groups.forEach { includeGroup ->
                    includeGroup.conditions.forEach { rule ->
                        when (rule) {
                            is TargetingRule.TimeSpentInApp -> inAppsWithTimer.add(
                                InAppWithTime(
                                    message,
                                    rule.timeSpentMillis
                                )
                            )

                            is TargetingRule.Event -> inAppsWithEvents.add(
                                InAppWithEvent(
                                    message,
                                    rule
                                )
                            )
                        }
                    }
                }
            } else {
                inAppsOnAppStart.add(message)
            }
        }

        if (inAppsWithTimer.isNotEmpty()) {
            sessionHandler.scheduleInAppMessages(inAppsWithTimer) { messagesToShow ->
                scope.launch { tryShowInAppFromList(messagesToShow.toMutableList()) }
            }
        }

        state.replaceWaitingForEvent(inAppsWithEvents)
        isFirstMessagesProcessed.value = true
        tryShowInAppFromList(inAppMessages = inAppsOnAppStart, showingOnAppStart = true)
    }

    private suspend fun tryShowInAppFromList(
        inAppMessages: MutableList<InAppMessage>,
        showingOnAppStart: Boolean = false
    ): Boolean {
        if (!showingOnAppStart) {
            updateSegmentStatuses(inAppMessages, updateCacheOnSuccess = true)
        }

        while (true) {
            if (inAppMessages.isEmpty()) break

            val inAppWithHighestId = inAppMessages.maxBy { it.messageId }
            val showedInApp = tryShowInApp(
                inAppMessage = inAppWithHighestId,
                showingOnAppStart = showingOnAppStart
            )

            if (showedInApp) {
                return true
            }
            inAppMessages.remove(inAppWithHighestId)
        }
        return false
    }

    private suspend fun updateSegmentStatuses(
        inAppMessages: List<InAppMessage>,
        updateCacheOnSuccess: Boolean = true
    ) {
        val messagesWithSegments = inAppMessages.filter {
            it.displayRules.async?.segment != null
        }
        val segmentIds =
            messagesWithSegments.mapNotNull { it.displayRules.async?.segment?.segmentId }.distinct()
        val segmentResponses = iamRepository.checkUserInSegments(segmentIds)

        val updatedMessages = mutableListOf<InAppMessage>()
        messagesWithSegments.forEach { message ->
            val segment = message.displayRules.async?.segment
            if (segment != null) {
                val checkResult = segmentResponses.firstOrNull { result ->
                    result.segmentId == segment.segmentId
                }
                if (checkResult != null) {
                    segment.isInSegment = checkResult.checkResult ?: false
                    segment.retryParams = checkResult.error?.toDomain()
                    segment.lastCheckedTimestamp = System.currentTimeMillis()
                    updatedMessages.add(message)
                }
            }
        }

        if (updateCacheOnSuccess) {
            iamRepository.updateInAppMessages(updatedMessages)
        }
    }

    private fun tryShowInApp(
        inAppMessage: InAppMessage,
        showingOnAppStart: Boolean = false
    ): Boolean {
        val decision = displayRules.decide(
            message = inAppMessage,
            sessionStartTimestamp = sessionHandler.getSessionStartTimestamp(),
            showingOnAppStart = showingOnAppStart
        )
        if (decision != DisplayDecision.SHOW) {
            return false
        }

        showInApp(inAppMessage)
        return true
    }

    private fun showInApp(inAppMessage: InAppMessage) {
        if (state.postponeIfPaused(inAppMessage)) return
        if (!canShowInApp()) return
        scope.launch { _inAppMessage.send(inAppMessage) }
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