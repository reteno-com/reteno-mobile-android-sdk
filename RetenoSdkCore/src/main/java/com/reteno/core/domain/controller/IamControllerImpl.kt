package com.reteno.core.domain.controller

import androidx.annotation.VisibleForTesting
import com.reteno.core.data.local.mappers.toDomain
import com.reteno.core.data.remote.model.iam.displayrules.frequency.FrequencyRuleValidator
import com.reteno.core.data.remote.model.iam.displayrules.schedule.ScheduleRuleValidator
import com.reteno.core.data.remote.model.iam.displayrules.targeting.InAppWithEvent
import com.reteno.core.data.remote.model.iam.displayrules.targeting.InAppWithTime
import com.reteno.core.data.remote.model.iam.displayrules.targeting.RuleEventValidator
import com.reteno.core.data.remote.model.iam.displayrules.targeting.TargetingRule
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent
import com.reteno.core.data.repository.IamRepository
import com.reteno.core.domain.ResultDomain
import com.reteno.core.domain.model.event.Event
import com.reteno.core.features.iam.IamJsEvent
import com.reteno.core.features.iam.InAppPauseBehaviour
import com.reteno.core.lifecycle.RetenoSessionHandler
import com.reteno.core.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicBoolean

internal class IamControllerImpl(
    private val iamRepository: IamRepository,
    eventController: EventController,
    private val sessionHandler: RetenoSessionHandler,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : IamController {

    private val isPausedInAppMessages = AtomicBoolean(false)
    private var pauseBehaviour = InAppPauseBehaviour.POSTPONE_IN_APPS

    private var interactionId: String? = null
    private val _fullHtmlStateFlow: MutableStateFlow<ResultDomain<String>> =
        MutableStateFlow(ResultDomain.Idle)
    override val fullHtmlStateFlow: StateFlow<ResultDomain<String>> = _fullHtmlStateFlow

    @VisibleForTesting
    var inAppsWaitingForEvent: MutableList<InAppWithEvent>? = null
        private set
    private var htmlJob: Job? = null

    private val _inAppMessage = MutableSharedFlow<InAppMessage>(
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val postponedNotifications = mutableListOf<InAppMessage>()
    override val inAppMessagesFlow: SharedFlow<InAppMessage> = _inAppMessage

    init {
        eventController.eventFlow
            .onEach { notifyEventOccurred(it) }
            .launchIn(scope)
        preloadHtml()
    }

    override fun fetchIamFullHtml(interactionId: String) {
        /*@formatter:off*/ Logger.i(TAG, "fetchIamFullHtml(): ", "widgetId = [", this.interactionId, "]")
        /*@formatter:on*/
        this.interactionId = interactionId
        _fullHtmlStateFlow.value = ResultDomain.Loading

        htmlJob = scope.launch {
            try {
                withTimeout(TIMEOUT) {
                    val baseHtml = async { iamRepository.getBaseHtml() }
                    val widget = async { iamRepository.getWidgetRemote(interactionId) }

                    val fullHtml = baseHtml.await().run {
                        widget.await().let { widgetModel ->
                            var text = this
                            widgetModel.model?.let {
                                text = text.replace(KEY_DOCUMENT_MODEL, it)
                            }
                            widgetModel.personalization?.let {
                                text = text.replace(KEY_PERSONALISATION, it)
                            }
                            text
                        }
                    }
                    _fullHtmlStateFlow.value = ResultDomain.Success(fullHtml)
                }
            } catch (e: TimeoutCancellationException) {
                _fullHtmlStateFlow.value =
                    ResultDomain.Error("fetchIamFullHtml(): widgetId = [${this@IamControllerImpl.interactionId}] TIMEOUT")
            }
        }
    }

    override fun fetchIamFullHtml(messageContent: InAppMessageContent?) {
        /*@formatter:off*/ Logger.i(TAG, "fetchIamFullHtml(): ", "widgetId = [", this.interactionId, "]")
        /*@formatter:on*/
        _fullHtmlStateFlow.value = ResultDomain.Loading

        scope.launch {
            try {
                withTimeout(TIMEOUT) {
                    val baseHtml = iamRepository.getBaseHtml()
                    val widgetModel = messageContent?.model.toString()

                    var text = baseHtml
                    text = text.replace(KEY_DOCUMENT_MODEL, widgetModel)
                    text = text.replace(KEY_PERSONALISATION, "{}")

                    _fullHtmlStateFlow.value = ResultDomain.Success(text)
                }
            } catch (e: TimeoutCancellationException) {
                _fullHtmlStateFlow.value =
                    ResultDomain.Error("fetchIamFullHtml(): widgetId = [${this@IamControllerImpl.interactionId}] TIMEOUT")
            }
        }
    }

    override fun widgetInitFailed(jsEvent: IamJsEvent) {
        /*@formatter:off*/ Logger.i(TAG, "widgetInitFailed(): ", "widgetId = [", interactionId, "], jsEvent = [", jsEvent, "]")
        /*@formatter:on*/
        interactionId?.let {
            iamRepository.widgetInitFailed(it, jsEvent)
        }
    }

    override fun reset() {
        _fullHtmlStateFlow.value = ResultDomain.Idle
        interactionId = null
        htmlJob?.cancel()
        htmlJob = null
    }

    override fun getInAppMessages() {
        scope.launch {
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
                e.printStackTrace()
            }
        }
    }

    override fun refreshSegmentation() {
        scope.launch {
            val messageListModel = iamRepository.getInAppMessages()
            val inAppMessages = messageListModel.messages

            updateSegmentStatuses(
                inAppMessages,
                updateCacheOnSuccess = messageListModel.isFromRemote.not()
            )
        }
    }

    private fun notifyEventOccurred(event: Event) {
        val inapps = inAppsWaitingForEvent
        if (inapps.isNullOrEmpty()) return

        val inAppsWithCurrentEvent = inapps.filter { inapp ->
            inapp.event.name == event.eventTypeKey
        }

        val validator = RuleEventValidator()
        val inAppsMatchingEventParams = inAppsWithCurrentEvent.filter { inapp ->
            validator.checkEventMatchesRules(inapp, event)
        }

        tryShowInAppFromList(inAppsMatchingEventParams.map { it.inApp }.toMutableList())
    }

    override fun pauseInAppMessages(isPaused: Boolean) {
        val wasDisabled = isPausedInAppMessages.get()
        isPausedInAppMessages.set(isPaused)
        if (wasDisabled && !isPaused) {
            showPostponedNotifications()
        }
    }

    override fun updateInAppMessage(inAppMessage: InAppMessage) {
        scope.launch {
            withContext(Dispatchers.IO) {
                iamRepository.updateInAppMessages(listOf(inAppMessage))
            }
        }
    }

    override fun setPauseBehaviour(behaviour: InAppPauseBehaviour) {
        pauseBehaviour = behaviour
    }

    private fun showPostponedNotifications() {
        when (pauseBehaviour) {
            InAppPauseBehaviour.SKIP_IN_APPS -> postponedNotifications.clear()
            InAppPauseBehaviour.POSTPONE_IN_APPS -> {
                postponedNotifications.firstOrNull()?.let(::showInApp)
                postponedNotifications.clear()
            }
        }
    }

    private fun sortMessages(inAppMessages: List<InAppMessage>) {
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
                tryShowInAppFromList(messagesToShow.toMutableList())
            }
        }

        if (inAppsWithEvents.isNotEmpty()) {
            inAppsWaitingForEvent = inAppsWithEvents
        }

        tryShowInAppFromList(inAppMessages = inAppsOnAppStart, showingOnAppStart = true)
    }

    private fun tryShowInAppFromList(
        inAppMessages: MutableList<InAppMessage>,
        showingOnAppStart: Boolean = false
    ) {
        scope.launch {
            if (!showingOnAppStart) {
                updateSegmentStatuses(inAppMessages, updateCacheOnSuccess = true)
            }

            val frequencyValidator = FrequencyRuleValidator()
            val scheduleValidator = ScheduleRuleValidator()
            while (true) {
                if (inAppMessages.isEmpty()) break

                val inAppWithHighestId = inAppMessages.maxBy { it.messageId }
                val showedInApp = tryShowInApp(
                    inAppMessage = inAppWithHighestId,
                    frequencyValidator = frequencyValidator,
                    scheduleValidator = scheduleValidator,
                    showingOnAppStart = showingOnAppStart
                )

                if (showedInApp) {
                    break
                }
                inAppMessages.remove(inAppWithHighestId)
            }
        }
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
        frequencyValidator: FrequencyRuleValidator = FrequencyRuleValidator(),
        scheduleValidator: ScheduleRuleValidator = ScheduleRuleValidator(),
        showingOnAppStart: Boolean = false
    ): Boolean {
        val content = inAppMessage.content
        if (content == null || content.model.isJsonNull) {
            return false
        }
        if (checkSegmentRuleMatches(inAppMessage).not()) {
            return false
        }

        if (!frequencyValidator.checkInAppMatchesFrequencyRules(
                inAppMessage = inAppMessage,
                sessionStartTimestamp = sessionHandler.getSessionStartTimestamp(),
                showingOnAppStart = showingOnAppStart
            )
        ) {
            return false
        }

        if (!scheduleValidator.checkInAppMatchesScheduleRules(inAppMessage)) {
            return false
        }

        showInApp(inAppMessage)
        return true
    }

    private fun showInApp(inAppMessage: InAppMessage) {
        if (isPausedInAppMessages.get()) postponedNotifications.add(inAppMessage)
        if (!canShowInApp()) return
        scope.launch { _inAppMessage.emit(inAppMessage) }
    }

    private fun checkSegmentRuleMatches(inAppMessage: InAppMessage): Boolean {
        return inAppMessage.displayRules.async?.segment?.isInSegment ?: true
    }

    private fun canShowInApp(): Boolean {
        return isPausedInAppMessages.get().not() && _fullHtmlStateFlow.value == ResultDomain.Idle
    }

    private fun preloadHtml() = scope.launch {
        iamRepository.getBaseHtml()
    }

    companion object {
        private val TAG: String = IamControllerImpl::class.java.simpleName

        internal const val TIMEOUT = 30_000L
        const val KEY_DOCUMENT_MODEL = "\${documentModel}"
        const val KEY_PERSONALISATION = "\${personalisation}"
    }
}