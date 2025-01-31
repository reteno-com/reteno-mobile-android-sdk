package com.reteno.core.domain.controller

import androidx.annotation.VisibleForTesting
import com.reteno.core.RetenoImpl
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private val _fullHtmlStateFlow: MutableStateFlow<ResultDomain<String>> =
        MutableStateFlow(ResultDomain.Idle)
    override val fullHtmlStateFlow: StateFlow<ResultDomain<String>> = _fullHtmlStateFlow

    @VisibleForTesting
    var inAppsWaitingForEvent: MutableList<InAppWithEvent>? = null
        private set
    private var htmlJob: Job? = null

    private val _inAppMessage = MutableSharedFlow<InAppMessage>(
        extraBufferCapacity = 2,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val postponedNotifications = mutableListOf<InAppMessage>()
    override val inAppMessagesFlow: SharedFlow<InAppMessage> = _inAppMessage

    init {
        eventController.eventFlow
            .onEach { notifyEventOccurred(it) }
            .launchIn(scope)
    }

    override fun fetchIamFullHtml(interactionId: String) {
        /*@formatter:off*/ Logger.i(TAG, "fetchIamFullHtml(): ", "widgetId = [", interactionId, "]")
        /*@formatter:on*/
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
                    val baseHtml = iamRepository.getBaseHtml()
                    val widgetModel = TEMP_MODEL

                    var text = baseHtml
                    text = text.replace(KEY_DOCUMENT_MODEL, widgetModel)
                    text = text.replace(KEY_PERSONALISATION, "{}")

                    _fullHtmlStateFlow.value = ResultDomain.Success(text)
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
                Logger.e(TAG, "getInAppMessages():", e)
            }
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

    override fun preloadHtml() {
        scope.launch {
            iamRepository.getBaseHtml()
        }
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


    companion object {
        private val TAG: String = IamControllerImpl::class.java.simpleName

        internal const val TIMEOUT = 30_000L
        const val KEY_DOCUMENT_MODEL = "\${documentModel}"
        const val KEY_PERSONALISATION = "\${personalisation}"
    }
}


val TEMP_MODEL = """
    {
      'views': [
        {
          'id': '7f6a16bb-fe07-4e01-9311-16cbf5974ef6',
          'path': [
            0
          ],
          'label': 'DEFAULT_PAGE',
          'type': 'VIEW',
          'name': 'SysContainerComponent',
          'version': '1.0.0',
          'props': {
            'isResponsive': true,
            'adaptiveStyles': {
              'desktop': [
                {
                  'element': 'host',
                  'styleAttributes': {},
                  'classes': ''
                }
              ],
              'mobile': [
                {
                  'element': 'host',
                  'styleAttributes': {
                    'width': 'auto',
                    'minWidth': null,
                    'maxWidth': '640px',
                    'height': '68px',
                    'minHeight': '100%',
                    'flexDirection': 'column',
                    'justifyContent': 'center',
                    'alignItems': 'center',
                    'gap': '30px',
                    'paddingTop': '0px',
                    'paddingBottom': '0px',
                    'paddingLeft': '0px',
                    'paddingRight': '5px',
                    'marginTop': '0px',
                    'marginBottom': '0px',
                    'marginLeft': '0px',
                    'marginRight': '0px',
                    'background': 'rgba(255, 255, 255, 0.8)',
                    'borderTopWidth': '0px',
                    'borderBottomWidth': '0px',
                    'borderLeftWidth': '0px',
                    'borderRightWidth': '0px',
                    'borderTopColor': 'rgb(0, 0, 0)',
                    'borderLeftColor': 'rgb(0, 0, 0)',
                    'borderBottomColor': 'rgb(0, 0, 0)',
                    'borderRightColor': 'rgb(0, 0, 0)',
                    'borderTopLeftRadius': '21px',
                    'borderTopRightRadius': '21px',
                    'borderBottomLeftRadius': '21px',
                    'borderBottomRightRadius': '21px',
                    'boxShadow': 'none',
                    'borderTopStyle': 'solid',
                    'borderBottomStyle': 'solid',
                    'borderLeftStyle': 'solid',
                    'borderRightStyle': 'solid',
                    '_marginEnabled': false,
                    '_paddingEnabled': true
                  },
                  'classes': ''
                }
              ]
            }
          },
          'children': [
            {
              'path': [
                0,
                0
              ],
              'id': 'cbfb0014-f5bc-43a1-b328-c9aaf24341eb',
              'type': 'COLUMNS',
              'name': 'SysColumnsComponent',
              'version': '1.0.0',
              'props': {
                'adaptiveStyles': {
                  'desktop': [
                    {
                      'element': 'host',
                      'styleAttributes': {
                        'position': 'relative',
                        'width': '100%',
                        'minWidth': '50px',
                        'height': 'auto',
                        'gap': '10px'
                      }
                    }
                  ],
                  'mobile': [
                    {
                      'element': 'host',
                      'styleAttributes': {
                        'position': 'relative',
                        'width': '100%',
                        'minWidth': null,
                        'height': 'auto',
                        'gap': '5px',
                        'flexDirection': 'column',
                        'justifyContent': 'center',
                        'alignItems': 'center'
                      }
                    }
                  ]
                },
                'isResponsive': false
              },
              'children': [
                {
                  'path': [
                    0,
                    0,
                    0
                  ],
                  'id': '9b7c2d37-afb0-4b78-9ee2-253cc001adee',
                  'type': 'COLUMN',
                  'name': 'SysColumnComponent',
                  'focusParentOnClick': false,
                  'preventDraggable': true,
                  'recursiveRemove': true,
                  'version': '1.0.0',
                  'props': {
                    'adaptiveStyles': {
                      'desktop': [
                        {
                          'element': 'host',
                          'styleAttributes': {
                            'display': 'inline-flex',
                            'position': 'relative',
                            'width': 'auto',
                            'minWidth': 'min-content',
                            'height': '100%',
                            'flexDirection': 'column',
                            'justifyContent': 'center',
                            'alignItems': 'center',
                            'gap': '10px',
                            'paddingTop': '10px',
                            'paddingBottom': '10px',
                            'paddingLeft': '10px',
                            'paddingRight': '10px',
                            'marginTop': '0px',
                            'marginBottom': '0px',
                            'marginLeft': '0px',
                            'marginRight': '0px',
                            'borderTopWidth': '0px',
                            'borderTopColor': 'rgb(0, 0, 0)',
                            'borderBottomWidth': '0px',
                            'borderBottomColor': 'rgb(0, 0, 0)',
                            'borderLeftWidth': '0px',
                            'borderLeftColor': 'rgb(0, 0, 0)',
                            'borderRightWidth': '0px',
                            'borderRightColor': 'rgb(0, 0, 0)',
                            'borderTopLeftRadius': '0px',
                            'borderTopRightRadius': '0px',
                            'borderBottomLeftRadius': '0px',
                            'borderBottomRightRadius': '0px',
                            'boxShadow': 'none',
                            'flexBasis': '0',
                            'borderTopStyle': 'solid',
                            'borderBottomStyle': 'solid',
                            'borderLeftStyle': 'solid',
                            'borderRightStyle': 'solid',
                            '_marginEnabled': false,
                            '_paddingEnabled': true
                          },
                          'classes': ''
                        }
                      ],
                      'mobile': [
                        {
                          'element': 'host',
                          'styleAttributes': {
                            'display': 'inline-flex',
                            'position': 'relative',
                            'width': 'auto',
                            'minWidth': 'min-content',
                            'height': '100%',
                            'flexDirection': 'column',
                            'justifyContent': 'center',
                            'alignItems': 'flex-end',
                            'gap': '0px',
                            'paddingTop': '10px',
                            'paddingBottom': '10px',
                            'paddingLeft': '10px',
                            'paddingRight': '0px',
                            'marginTop': '0px',
                            'marginBottom': '0px',
                            'marginLeft': '0px',
                            'marginRight': '0px',
                            'borderTopWidth': '0px',
                            'borderTopColor': 'rgb(0, 0, 0)',
                            'borderBottomWidth': '0px',
                            'borderBottomColor': 'rgb(0, 0, 0)',
                            'borderLeftWidth': '0px',
                            'borderLeftColor': 'rgb(0, 0, 0)',
                            'borderRightWidth': '0px',
                            'borderRightColor': 'rgb(0, 0, 0)',
                            'borderTopLeftRadius': '0px',
                            'borderTopRightRadius': '0px',
                            'borderBottomLeftRadius': '0px',
                            'borderBottomRightRadius': '0px',
                            'boxShadow': 'none',
                            'flexBasis': '0',
                            'borderTopStyle': 'solid',
                            'borderBottomStyle': 'solid',
                            'borderLeftStyle': 'solid',
                            'borderRightStyle': 'solid',
                            '_marginEnabled': false,
                            '_paddingEnabled': true
                          },
                          'classes': ''
                        }
                      ]
                    },
                    'content': {
                      'size': 0.4016
                    }
                  },
                  'children': [
                    {
                      'id': '970d2367-9901-4610-9794-3c7a5975e93b',
                      'name': 'SysImageComponent',
                      'path': [
                        0,
                        0,
                        0,
                        0
                      ],
                      'type': 'IMAGE',
                      'props': {
                        'control': {
                          'imageSource': {
                            'url': 'https://cdn.claspo.io/img/1012/forms/42062/b2b14b40-19af-4019-9e37-dab3f839835f.svg',
                            'externalSource': false
                          }
                        },
                        'adaptiveStyles': {
                          'mobile': [
                            {
                              'classes': '',
                              'element': 'host',
                              'styleAttributes': {
                                'width': '100%',
                                'height': '39px',
                                'display': 'block',
                                'maxWidth': '100%',
                                'minWidth': 'min-content',
                                'maxHeight': '100%',
                                'minHeight': '39px',
                                'paddingTop': '0px',
                                'paddingLeft': '0px',
                                'paddingRight': '0px',
                                'paddingBottom': '0px',
                                '_paddingEnabled': false
                              }
                            },
                            {
                              'classes': '',
                              'element': 'image',
                              'styleAttributes': {
                                'objectFit': 'contain',
                                'background': 'transparent',
                                'borderTopColor': 'rgb(0, 0, 0)',
                                'borderTopStyle': 'solid',
                                'borderTopWidth': '0px',
                                'borderLeftColor': 'rgb(0, 0, 0)',
                                'borderLeftStyle': 'solid',
                                'borderLeftWidth': '0px',
                                'borderRightColor': 'rgb(0, 0, 0)',
                                'borderRightStyle': 'solid',
                                'borderRightWidth': '0px',
                                'borderBottomColor': 'rgb(0, 0, 0)',
                                'borderBottomStyle': 'solid',
                                'borderBottomWidth': '0px',
                                'borderTopLeftRadius': '0px',
                                'borderTopRightRadius': '0px',
                                'borderBottomLeftRadius': '0px',
                                'borderBottomRightRadius': '0px'
                              }
                            }
                          ],
                          'desktop': [
                            {
                              'classes': '',
                              'element': 'host',
                              'styleAttributes': {
                                'width': '100%',
                                'height': '38px',
                                'display': 'block',
                                'maxWidth': '100%',
                                'minWidth': 'min-content',
                                'maxHeight': '100%',
                                'minHeight': '38px',
                                'paddingTop': '0px',
                                'paddingLeft': '0px',
                                'paddingRight': '0px',
                                'paddingBottom': '0px',
                                '_paddingEnabled': false
                              }
                            },
                            {
                              'classes': '',
                              'element': 'image',
                              'styleAttributes': {
                                'objectFit': 'contain',
                                'background': 'transparent',
                                'borderTopColor': 'rgb(0, 0, 0)',
                                'borderTopStyle': 'solid',
                                'borderTopWidth': '0px',
                                'borderLeftColor': 'rgb(0, 0, 0)',
                                'borderLeftStyle': 'solid',
                                'borderLeftWidth': '0px',
                                'borderRightColor': 'rgb(0, 0, 0)',
                                'borderRightStyle': 'solid',
                                'borderRightWidth': '0px',
                                'borderBottomColor': 'rgb(0, 0, 0)',
                                'borderBottomStyle': 'solid',
                                'borderBottomWidth': '0px',
                                'borderTopLeftRadius': '0px',
                                'borderTopRightRadius': '0px',
                                'borderBottomLeftRadius': '0px',
                                'borderBottomRightRadius': '0px'
                              }
                            }
                          ]
                        }
                      },
                      'version': '1.0.0'
                    }
                  ]
                },
                {
                  'path': [
                    0,
                    0,
                    1
                  ],
                  'id': '2175a970-1d7d-4ec5-b8f6-22b3602036c7',
                  'type': 'COLUMN',
                  'name': 'SysColumnComponent',
                  'focusParentOnClick': false,
                  'preventDraggable': true,
                  'recursiveRemove': true,
                  'version': '1.0.0',
                  'props': {
                    'adaptiveStyles': {
                      'desktop': [
                        {
                          'element': 'host',
                          'styleAttributes': {
                            'display': 'inline-flex',
                            'position': 'relative',
                            'width': 'auto',
                            'minWidth': 'min-content',
                            'height': '100%',
                            'flexDirection': 'column',
                            'justifyContent': 'center',
                            'alignItems': 'center',
                            'gap': '10px',
                            'paddingTop': '10px',
                            'paddingBottom': '10px',
                            'paddingLeft': '10px',
                            'paddingRight': '10px',
                            'marginTop': '0px',
                            'marginBottom': '0px',
                            'marginLeft': '0px',
                            'marginRight': '0px',
                            'borderTopWidth': '0px',
                            'borderTopColor': 'rgb(0, 0, 0)',
                            'borderBottomWidth': '0px',
                            'borderBottomColor': 'rgb(0, 0, 0)',
                            'borderLeftWidth': '0px',
                            'borderLeftColor': 'rgb(0, 0, 0)',
                            'borderRightWidth': '0px',
                            'borderRightColor': 'rgb(0, 0, 0)',
                            'borderTopLeftRadius': '0px',
                            'borderTopRightRadius': '0px',
                            'borderBottomLeftRadius': '0px',
                            'borderBottomRightRadius': '0px',
                            'boxShadow': 'none',
                            'flexBasis': '0',
                            'borderTopStyle': 'solid',
                            'borderBottomStyle': 'solid',
                            'borderLeftStyle': 'solid',
                            'borderRightStyle': 'solid',
                            '_marginEnabled': false,
                            '_paddingEnabled': true
                          },
                          'classes': ''
                        }
                      ],
                      'mobile': [
                        {
                          'element': 'host',
                          'styleAttributes': {
                            'display': 'inline-flex',
                            'position': 'relative',
                            'width': 'auto',
                            'minWidth': 'min-content',
                            'height': '100%',
                            'flexDirection': 'column',
                            'justifyContent': 'center',
                            'alignItems': 'flex-start',
                            'gap': '0px',
                            'paddingTop': '10px',
                            'paddingBottom': '10px',
                            'paddingLeft': '10px',
                            'paddingRight': '10px',
                            'marginTop': '0px',
                            'marginBottom': '0px',
                            'marginLeft': '0px',
                            'marginRight': '0px',
                            'borderTopWidth': '0px',
                            'borderTopColor': 'rgb(0, 0, 0)',
                            'borderBottomWidth': '0px',
                            'borderBottomColor': 'rgb(0, 0, 0)',
                            'borderLeftWidth': '0px',
                            'borderLeftColor': 'rgb(0, 0, 0)',
                            'borderRightWidth': '0px',
                            'borderRightColor': 'rgb(0, 0, 0)',
                            'borderTopLeftRadius': '0px',
                            'borderTopRightRadius': '0px',
                            'borderBottomLeftRadius': '0px',
                            'borderBottomRightRadius': '0px',
                            'boxShadow': 'none',
                            'flexBasis': '0',
                            'borderTopStyle': 'solid',
                            'borderBottomStyle': 'solid',
                            'borderLeftStyle': 'solid',
                            'borderRightStyle': 'solid',
                            '_marginEnabled': false,
                            '_paddingEnabled': false
                          },
                          'classes': ''
                        }
                      ]
                    },
                    'content': {
                      'size': 1.6048
                    }
                  },
                  'children': [
                    {
                      'path': [
                        0,
                        0,
                        1,
                        0
                      ],
                      'id': '40281b61-1cd1-4302-8aa6-fff59dfe1b09',
                      'type': 'TEXT',
                      'name': 'SysTextComponent',
                      'version': '1.0.0',
                      'props': {
                        'adaptiveStyles': {
                          'desktop': [
                            {
                              'element': 'host',
                              'styleAttributes': {
                                'width': '100%',
                                'height': 'auto',
                                'maxWidth': '100%',
                                'minWidth': 'min-content',
                                'marginTop': '0px',
                                'maxHeight': '100%',
                                'minHeight': null,
                                'marginLeft': '0px',
                                'marginRight': '0px',
                                'marginBottom': '45px',
                                '_marginEnabled': false,
                                'fontFamily': 'Montserrat',
                                'paddingBottom': '0px',
                                '_paddingEnabled': true,
                                'paddingTop': '0px'
                              },
                              'classes': null
                            },
                            {
                              'classes': null,
                              'element': 'text',
                              'styleAttributes': {
                                'paddingTop': '0px',
                                'paddingLeft': '0px',
                                'paddingRight': '0px',
                                'paddingBottom': '0px',
                                '_paddingEnabled': false,
                                'color': 'rgba(0, 0, 0, 1)',
                                'fontSize': '17px',
                                'textAlign': 'start',
                                'fontFamily': 'Montserrat',
                                'fontWeight': '400',
                                'lineHeight': '120%',
                                'textShadow': 'none',
                                'letterSpacing': '0px',
                                'textTransform': 'none'
                              },
                              'placeholderStyleAttributes': {
                                'color': 'rgb(0, 0, 0)'
                              }
                            }
                          ],
                          'mobile': [
                            {
                              'element': 'host',
                              'styleAttributes': {
                                'width': '100%',
                                'height': 'auto',
                                'maxWidth': '100%',
                                'minWidth': 'min-content',
                                'marginTop': '0px',
                                'maxHeight': '100%',
                                'minHeight': null,
                                'marginLeft': '0px',
                                'marginRight': '0px',
                                'marginBottom': '0px',
                                '_marginEnabled': false,
                                'fontFamily': 'Montserrat',
                                'paddingTop': '10px',
                                '_paddingEnabled': true,
                                'paddingBottom': '0px'
                              },
                              'classes': null
                            },
                            {
                              'classes': null,
                              'element': 'text',
                              'styleAttributes': {
                                'paddingTop': '0px',
                                'paddingLeft': '0px',
                                'paddingRight': '0px',
                                'paddingBottom': '0px',
                                '_paddingEnabled': false,
                                'color': 'rgba(0, 0, 0, 1)',
                                'fontSize': '20px',
                                'textAlign': 'start',
                                'fontFamily': 'Roboto',
                                'fontWeight': '400',
                                'lineHeight': '120%',
                                'textShadow': 'none',
                                'letterSpacing': '0px',
                                'textTransform': 'none'
                              },
                              'placeholderStyleAttributes': {
                                'color': 'rgb(0, 0, 0)'
                              }
                            }
                          ]
                        },
                        'content': {
                          'text': 'Get 30% off now',
                          'textContrastEnabled': true
                        }
                      }
                    }
                  ]
                }
              ]
            }
          ]
        }
      ],
      'shared': {
        'mobileBreakpointWidth': 576,
        'headerFontFamily': '',
        'textFontFamily': '',
        'textClasses': {
          'cl-text-class-h1': {
            'id': 'cl-text-class-h1',
            'name': 'Title',
            'isHeader': true,
            'styleAttributes': {
              'color': 'rgba(0, 0, 0, 1)',
              'textAlign': 'center',
              'lineHeight': '120%',
              'fontWeight': '700',
              'fontSize': '21px',
              'textShadow': 'none',
              'fontFamily': 'Montserrat',
              'letterSpacing': '0px',
              'textTransform': 'none'
            },
            'placeholderStyleAttributes': {
              'color': 'rgb(0, 0, 0)'
            }
          },
          'cl-text-class-h2': {
            'id': 'cl-text-class-h2',
            'name': 'Subtitle',
            'isHeader': true,
            'styleAttributes': {
              'color': 'rgb(0, 0, 0)',
              'textAlign': 'start',
              'lineHeight': '120%',
              'fontWeight': '400',
              'fontSize': '40px',
              'textShadow': 'none',
              'fontFamily': '',
              'letterSpacing': '0px'
            }
          },
          'cl-text-class-regular-text': {
            'id': 'cl-text-class-regular-text',
            'name': 'Regular text',
            'isHeader': false,
            'styleAttributes': {
              'color': 'rgba(0, 0, 0, 1)',
              'textAlign': 'center',
              'lineHeight': '120%',
              'fontWeight': '500',
              'fontSize': '14px',
              'textShadow': 'none',
              'fontFamily': 'Montserrat',
              'letterSpacing': '0px',
              'textTransform': 'none'
            },
            'placeholderStyleAttributes': {
              'color': 'rgb(0, 0, 0)'
            }
          },
          'cl-text-class-button': {
            'id': 'cl-text-class-button',
            'name': 'Button text',
            'isHeader': false,
            'styleAttributes': {
              'color': 'rgba(255, 255, 255, 1)',
              'textAlign': 'center',
              'lineHeight': '120%',
              'fontWeight': '700',
              'fontSize': '14px',
              'textShadow': 'none',
              'fontFamily': 'Montserrat',
              'letterSpacing': '0px',
              'textTransform': 'none'
            },
            'placeholderStyleAttributes': {
              'color': 'rgb(0, 0, 0)'
            }
          },
          'cl-text-class-input-label': {
            'id': 'cl-text-class-input-label',
            'name': 'Field title',
            'isHeader': false,
            'styleAttributes': {
              'color': 'rgb(0, 0, 0)',
              'textAlign': 'start',
              'lineHeight': '120%',
              'fontWeight': '400',
              'fontSize': '16px',
              'textShadow': 'none',
              'fontFamily': '',
              'letterSpacing': '0px'
            }
          },
          'cl-text-class-input-text': {
            'id': 'cl-text-class-input-text',
            'name': 'Field text',
            'isHeader': false,
            'styleAttributes': {
              'color': 'rgb(0, 0, 0)',
              'textAlign': 'start',
              'lineHeight': '120%',
              'fontWeight': '400',
              'fontSize': '16px',
              'textShadow': 'none',
              'fontFamily': '',
              'letterSpacing': '0px'
            }
          }
        },
        'googleFonts': [
          'Montserrat',
          'Roboto'
        ],
        'slideUp': {
          'mobile': {
            'position': 'TOP',
            'leftAndRightOffset': '40px',
            'verticalOffset': '50px'
          },
          'desktop': {
            'position': 'TOP',
            'leftAndRightOffset': '40px',
            'verticalOffset': '50px'
          }
        },
        'actualSize': {
          'desktop': {
            'width': '841px',
            'height': '700px'
          },
          'mobile': {
            'width': '203.578px',
            'height': '68px'
          }
        },
        'cssVars': {},
        'closeIcon': null,
        'linkParams': {
          'color': 'rgb(0, 0, 238)'
        }
      },
      'inAppConfig': {
        'layoutType': 'SLIDE_UP'
      }
    }

    
""".trimIndent()