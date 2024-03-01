package com.reteno.core.domain.controller

import android.util.Log
import com.reteno.core.data.remote.model.iam.displayrules.RuleRelation
import com.reteno.core.data.remote.model.iam.displayrules.StringOperator
import com.reteno.core.data.remote.model.iam.displayrules.frequency.FrequencyRule
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
import com.reteno.core.lifecycle.RetenoSessionHandler
import com.reteno.core.util.Logger
import com.reteno.core.view.iam.IamView
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class IamControllerImpl(
    private val iamRepository: IamRepository,
    private val sessionHandler: RetenoSessionHandler
) : IamController {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var interactionId: String? = null
    private val _fullHtmlStateFlow: MutableStateFlow<ResultDomain<String>> =
        MutableStateFlow(ResultDomain.Loading)
    override val fullHtmlStateFlow: StateFlow<ResultDomain<String>> = _fullHtmlStateFlow

    private var inAppsWaitingForEvent: MutableList<InAppWithEvent>? = null

    private var iamView: IamView? = null // TODO solution for debug, remove or put this into constructor

    override fun fetchIamFullHtml(interactionId: String) {
        /*@formatter:off*/ Logger.i(TAG, "fetchIamFullHtml(): ", "widgetId = [", this.interactionId, "]")
        /*@formatter:on*/
        this.interactionId = interactionId
        _fullHtmlStateFlow.value = ResultDomain.Loading

        scope.launch {
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
        _fullHtmlStateFlow.value = ResultDomain.Loading
        interactionId = null
        scope.coroutineContext.cancelChildren()
    }

    override fun setIamView(iamView: IamView) {
        this.iamView = iamView
    }

    override fun getInAppMessages(showMessage: (InAppMessage) -> Unit) {
        scope.launch {
            try {
                val inAppMessages = iamRepository.getInAppMessages().map {
                    InAppMessage(
                        it.messageId,
                        it.messageInstanceId,
                        it.parseRules()
                    )
                }

                val messagesWithSegments = inAppMessages.filter { it.displayRules.async?.segment?.segmentId != null }
                val segmentIds = messagesWithSegments.mapNotNull { it.displayRules.async?.segment?.segmentId }.distinct()

                val contentsDeferred = async { iamRepository.getInAppMessagesContent(inAppMessages.map { it.messageInstanceId })}
                val segmentDeferred = async { iamRepository.checkUserInSegments(segmentIds) }

                val contents = contentsDeferred.await()

                inAppMessages.forEach { message ->
                    message.content = contents.firstOrNull {
                        it.messageInstanceId == message.messageInstanceId
                    }
                    Log.e("ololo","found content for ${message.messageId} instance ${message.messageInstanceId}: ${message.content != null}")
                }

                val segmentResponses = segmentDeferred.await()
                messagesWithSegments.forEach { message ->
                    val segment = message.displayRules.async?.segment
                    segment?.let { segmentRule ->
                        segmentRule.isInSegment = segmentResponses.firstOrNull { checkResult ->
                            checkResult.segmentId == segmentRule.segmentId
                        }?.checkResult ?: false
                        Log.e("ololo","set segment for ${message.messageId}, segment id ${segmentRule.segmentId}: ${segmentRule.isInSegment}")
                    }
                }

                sortMessages(inAppMessages)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun notifyEventOccurred(event: Event) {
        Log.e("ololo","notifyEventOccurred ${event}")

        val inapps = inAppsWaitingForEvent
        if (inapps.isNullOrEmpty()) return

        val inAppsWithCurrentEvent = inapps.filter { inapp->
            inapp.event.name == event.eventTypeKey
        }

        val validator = RuleEventValidator()
        val inAppsMatchingEventParams = inAppsWithCurrentEvent.filter { inapp ->
            val result = validator.checkEventMatchesRules(inapp, event)
            Log.e("ololo","checkEventMatchesRules ${inapp.inApp.messageId} is matching $result")
            result
        }

        tryShowInAppFromList(inAppsMatchingEventParams.map { it.inApp }.toMutableList())
    }

    private fun sortMessages(inAppMessages: List<InAppMessage>) {
        val inAppsWithEvents = mutableListOf<InAppWithEvent>()
        val inAppsWithTimer = mutableListOf<InAppWithTime>()
        val inAppsOnAppStart = mutableListOf<InAppMessage>()

        inAppMessages.forEach {  message ->
            val includeRules = message.displayRules.targeting?.include
            if (includeRules != null) {
                includeRules.groups.forEach { includeGroup ->
                    includeGroup.conditions.forEach { rule ->
                        when (rule) {
                            is TargetingRule.TimeSpentInApp -> inAppsWithTimer.add(InAppWithTime(message, rule.timeSpentMillis))
                            is TargetingRule.Event -> inAppsWithEvents.add(InAppWithEvent(message, rule))
                        }
                    }
                }
            } else {
                inAppsOnAppStart.add(message)
            }
        }

        if (inAppsWithTimer.isNotEmpty()) {
            sessionHandler.scheduleInAppMessages(inAppsWithTimer) { messagesToShow ->
                Log.e("ololo","show inapp by time: ${messagesToShow.size}")
                tryShowInAppFromList(messagesToShow.toMutableList())
            }
        }

        if (inAppsWithEvents.isNotEmpty()) {
            inAppsWaitingForEvent = inAppsWithEvents
        }

        tryShowInAppFromList(inAppsOnAppStart)
    }

    private fun tryShowInAppFromList(inAppMessages: MutableList<InAppMessage>) {
        while (true) {
            if (inAppMessages.isEmpty()) return

            val inAppWithHighestId = inAppMessages.maxBy { it.messageId }
            Log.e("ololo","try show inapp ${inAppWithHighestId.messageId}")
            val showedInApp = tryShowInApp(inAppWithHighestId)

            if (showedInApp) {
                return
            }
            inAppMessages.remove(inAppWithHighestId)
        }
    }

    private fun tryShowInApp(inAppMessage: InAppMessage): Boolean {
        if (iamView == null || iamView?.isViewShown() == true) return false

        val frequency = inAppMessage.displayRules.frequency?.predicates?.firstOrNull()
        if (frequency != null && frequency != FrequencyRule.NoLimit && inAppMessage.alreadyShown) {
            return false
        }

        if (checkSegmentRuleMatches(inAppMessage).not()) {
            return false
        }

        inAppMessage.alreadyShown = true
        iamView?.initialize(inAppMessage)
        return true
    }

    private fun checkSegmentRuleMatches(inAppMessage: InAppMessage): Boolean {
        return inAppMessage.displayRules.async?.segment?.isInSegment ?: true
    }

    private fun findInAppToShowByTime(inAppMessages: List<InAppMessage>): InAppMessage? {
        var filteredMessages = inAppMessages// TODO filterInAppsByFrequency(inAppMessages)
        return if (filteredMessages.isEmpty()) {
            null
        } else {
            filteredMessages.maxBy { it.messageId }
        }
    }

    private fun filterInAppsByFrequency(inAppMessages: List<InAppMessage>): List<InAppMessage> {
        return inAppMessages.filter {
            val predicates = it.displayRules.frequency?.predicates
            if (predicates == null) {
                false
            } else {
                predicates.contains(FrequencyRule.NoLimit) ||
                        predicates.contains(FrequencyRule.OncePerApp)
            }
        }
    }

    companion object {
        private val TAG: String = IamControllerImpl::class.java.simpleName

        internal const val TIMEOUT = 30_000L
        const val KEY_DOCUMENT_MODEL = "\${documentModel}"
        const val KEY_PERSONALISATION = "\${personalisation}"
    }
}