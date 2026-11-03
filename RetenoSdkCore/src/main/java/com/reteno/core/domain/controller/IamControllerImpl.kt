package com.reteno.core.domain.controller

import androidx.annotation.VisibleForTesting
import com.google.gson.JsonObject
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUser
import com.reteno.core.data.local.mappers.toDomain
import com.reteno.core.data.local.model.BooleanDb
import com.reteno.core.data.remote.model.iam.displayrules.frequency.FrequencyRuleValidator
import com.reteno.core.data.remote.model.iam.displayrules.schedule.ScheduleRuleValidator
import com.reteno.core.data.remote.model.iam.displayrules.targeting.InAppWithEvent
import com.reteno.core.data.remote.model.iam.displayrules.targeting.InAppWithTime
import com.reteno.core.data.remote.model.iam.displayrules.targeting.RuleEventValidator
import com.reteno.core.data.remote.model.iam.displayrules.targeting.TargetingRule
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent
import com.reteno.core.data.remote.model.iam.message.InAppMessagesList
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutParams
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutParams.Position
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutType
import com.reteno.core.data.repository.IamRepository
import com.reteno.core.domain.ResultDomain
import com.reteno.core.domain.model.event.Event
import com.reteno.core.features.iam.IamJsEvent
import com.reteno.core.features.iam.InAppPauseBehaviour
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
import java.util.concurrent.atomic.AtomicBoolean

internal class IamControllerImpl(
    private val iamRepository: IamRepository,
    eventController: EventController,
    private val sessionHandler: RetenoSessionHandler,
    private val userDatabase: RetenoDatabaseManagerUser,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : IamController {

    @Volatile
    private var latestWorkflowMessages: List<InAppMessage> = emptyList()
    private val workflowPresentation = AtomicBoolean(false)
    private val isPausedInAppMessages = AtomicBoolean(false)
    private var pauseBehaviour = InAppPauseBehaviour.POSTPONE_IN_APPS

    private val _fullHtmlStateFlow: MutableStateFlow<ResultDomain<IamFetchResult>> =
        MutableStateFlow(ResultDomain.Idle)
    override val fullHtmlStateFlow: StateFlow<ResultDomain<IamFetchResult>> = _fullHtmlStateFlow

    @VisibleForTesting
    var inAppsWaitingForEvent: MutableList<InAppWithEvent>? = null
        private set
    private var htmlJob: Job? = null

    private val _inAppMessage = Channel<InAppMessage>(
        capacity = 2,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val postponedNotifications = mutableListOf<InAppMessage>()
    override val inAppMessagesFlow: Flow<InAppMessage>
        get() = _inAppMessage.receiveAsFlow()

    private val isFirstMessagesProcessed = MutableStateFlow(false)

    init {
        eventController.eventFlow
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

        htmlJob?.cancel()
        htmlJob = scope.launch {
            try {
                withTimeout(TIMEOUT) {
                    val baseHtml = iamRepository.getLocalBaseHtml()
                    var widgetModel = messageContent?.model.toString()
                    if (messageContent?.iid == null && widgetModel.contains(Regex("\\$\\{.+\\}"))) {
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
                    text = text.replace(KEY_PERSONALISATION, messageContent?.personalisation?.toString() ?: "{}")

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
        Logger.d(TAG, "widgetInitFailed(): ")
        /*@formatter:on*/
        iamRepository.widgetInitFailed(tenantId, jsEvent)
    }

    override fun canPresentWorkflow(message: InAppMessage): Boolean =
        !message.inWorkflow || (iamRepository.isWorkflowEligible(message) && hasCurrentWorkflowRules(message))

    private fun rememberWorkflowRules(result: InAppMessagesList) {
        if (result.isSuccessful) latestWorkflowMessages = result.messages.filter { it.inWorkflow }
    }

    private fun hasCurrentWorkflowRules(
        message: InAppMessage,
        evaluatedRules: JsonObject = message.displayRulesJson
    ): Boolean {
        val latest = latestWorkflowMessages.firstOrNull {
            it.workflowContext == message.workflowContext && it.iid == message.iid
        } ?: return true
        return latest.displayRulesJson.deepCopy().apply { remove("FREQUENCY") } ==
            evaluatedRules.deepCopy().apply { remove("FREQUENCY") }
    }

    override fun canResumeWorkflow(message: InAppMessage): Boolean =
        iamRepository.isWorkflowContextCurrent(message)

    override fun reset() {
        workflowPresentation.set(false)
        _fullHtmlStateFlow.value = ResultDomain.Idle
        htmlJob?.cancel()
        htmlJob = null
    }

    override suspend fun getInAppMessages() = coroutineScope {
        try {
            val messageListModel = iamRepository.getInAppMessages()
            val inAppMessages = messageListModel.messages
            rememberWorkflowRules(messageListModel)

            val messagesWithNoContent = inAppMessages.filter { !it.inWorkflow && it.content == null }
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
                rememberWorkflowRules(messageListModel)

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
        val inapps = inAppsWaitingForEvent
        if (inapps.isNullOrEmpty()) return false

        val inAppsWithCurrentEvent = inapps.filter { inapp ->
            inapp.event.name == event.eventTypeKey
        }

        val validator = RuleEventValidator()
        val inAppsMatchingEventParams = inAppsWithCurrentEvent.filter { inapp ->
            validator.checkEventMatchesRules(inapp, event)
        }

        return tryShowInAppFromList(inAppsMatchingEventParams.map { it.inApp }.toMutableList())
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

    override fun preloadHtml() {
        scope.launch {
            iamRepository.getBaseHtml()
        }
    }

    private fun showPostponedNotifications() {
        when (pauseBehaviour) {
            InAppPauseBehaviour.SKIP_IN_APPS -> postponedNotifications.clear()
            InAppPauseBehaviour.POSTPONE_IN_APPS -> {
                postponedNotifications.firstOrNull()?.let { message ->
                    if (message.inWorkflow) scope.launch { tryShowInAppFromList(mutableListOf(message)) }
                    else showInApp(message)
                }
                postponedNotifications.clear()
            }
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

        if (inAppsWithEvents.isNotEmpty()) {
            inAppsWaitingForEvent = inAppsWithEvents
        }
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

    private suspend fun tryShowInApp(
        inAppMessage: InAppMessage,
        frequencyValidator: FrequencyRuleValidator = FrequencyRuleValidator(),
        scheduleValidator: ScheduleRuleValidator = ScheduleRuleValidator(),
        showingOnAppStart: Boolean = false
    ): Boolean {
        val content = inAppMessage.content
        if (!inAppMessage.inWorkflow && (content == null || content.model.isJsonNull)) {
            return false
        }
        if (checkSegmentRuleMatches(inAppMessage).not()) {
            return false
        }

        if (!inAppMessage.inWorkflow && !frequencyValidator.checkInAppMatchesFrequencyRules(
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

        if (inAppMessage.inWorkflow) {
            if (!canPresentWorkflow(inAppMessage)) return false
            if (isPausedInAppMessages.get()) {
                postponedNotifications.add(inAppMessage)
                return false
            }
            if (!canShowInApp() || !workflowPresentation.compareAndSet(false, true)) return false
            val evaluatedRules = inAppMessage.displayRulesJson.deepCopy().apply { remove("FREQUENCY") }
            try {
                inAppMessage.content = iamRepository.getWorkflowContent(listOf(inAppMessage)).firstOrNull {
                    it.iid == inAppMessage.iid && it.messageInstanceId == inAppMessage.messageInstanceId
                }
                if (inAppMessage.content == null || inAppMessage.content?.model?.isJsonNull != false ||
                    !iamRepository.isWorkflowEligible(inAppMessage) || !hasCurrentWorkflowRules(inAppMessage, evaluatedRules)) {
                    workflowPresentation.set(false)
                    return false
                }
            } catch (error: Exception) {
                workflowPresentation.set(false)
                throw error
            }
        }
        val shown = showInApp(inAppMessage)
        if (inAppMessage.inWorkflow && !shown) workflowPresentation.set(false)
        return if (inAppMessage.inWorkflow) shown else true
    }

    private fun showInApp(inAppMessage: InAppMessage): Boolean {
        if (isPausedInAppMessages.get()) postponedNotifications.add(inAppMessage)
        if (!canShowInApp()) return false
        scope.launch { _inAppMessage.send(inAppMessage) }
        return true
    }

    private fun checkSegmentRuleMatches(inAppMessage: InAppMessage): Boolean {
        val segment = inAppMessage.displayRules.async?.segment
        return if (inAppMessage.inWorkflow && segment != null) segment.isInSegment == true else segment?.isInSegment ?: true
    }

    private fun canShowInApp(): Boolean {
        return isPausedInAppMessages.get().not() && _fullHtmlStateFlow.value == ResultDomain.Idle
    }


    companion object {
        private val TAG: String = IamControllerImpl::class.java.simpleName

        internal const val TIMEOUT = 30_000L
        const val KEY_DOCUMENT_MODEL = "\${documentModel}"
        const val KEY_PERSONALISATION = "\${personalisation}"
    }
}
