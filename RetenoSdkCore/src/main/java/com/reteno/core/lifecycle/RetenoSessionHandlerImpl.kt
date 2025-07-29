package com.reteno.core.lifecycle

import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.data.remote.model.iam.displayrules.targeting.InAppWithTime
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.lifecycle.RetenoSessionHandler.SessionEvent
import com.reteno.core.lifecycle.RetenoSessionHandler.SessionEvent.SessionStartEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

internal class RetenoSessionHandlerImpl(
    private val sharedPrefsManager: SharedPrefsManager
) : RetenoSessionHandler {

    override val sessionEventFlow = MutableSharedFlow<SessionEvent>(
        extraBufferCapacity = 2,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
        replay = 2
    )

    private var foregroundTimeMillis: Long = 0L
    private var sessionStartTimestamp: Long = 0L
    private var sessionId = ""

    private var timeSinceResume: Long = 0L
    private var appResumedTimestamp: Long = 0L

    private var previousForegroundTime: Long = 0L
    private var appPausedTimestamp: Long = -1L

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var scheduledMessages: MutableList<InAppWithTime>? = null
    private var closestScheduledMessages: List<InAppWithTime>? = null
    private var showInApp: ((List<InAppMessage>) -> Unit)? = null

    override fun start() {
        initSession()
        job = scope.launch {
            while (true) {
                countTime()
                delay(TIME_COUNTER_DELAY)
            }
        }
    }

    override fun stop() {
        sharedPrefsManager.saveBackgroundCount(sharedPrefsManager.getBackgroundCount() + 1)
        appPausedTimestamp = System.currentTimeMillis()
        sharedPrefsManager.saveAppSessionTime(foregroundTimeMillis)
        sharedPrefsManager.saveAppStoppedTimestamp(appPausedTimestamp)
        job?.cancel()
        job = null
    }

    override fun scheduleInAppMessages(
        messages: MutableList<InAppWithTime>,
        onTimeMatch: (List<InAppMessage>) -> Unit
    ) {
        if (messages.isEmpty()) return

        showInApp = onTimeMatch
        scheduledMessages = messages
        findNextScheduledMessages()
    }

    override fun getForegroundTimeMillis(): Long {
        return foregroundTimeMillis
    }

    override fun getSessionStartTimestamp(): Long {
        return sessionStartTimestamp
    }

    override fun getSessionId(): String {
        return sessionId
    }

    private fun findNextScheduledMessages() {
        var messages = scheduledMessages

        if (messages.isNullOrEmpty()) {
            closestScheduledMessages = null
            return
        }

        messages.sortBy { it.time }

        val nextMessageTime = messages.first().time
        messages = messages.filter { it.time == nextMessageTime }.toMutableList()

        closestScheduledMessages = messages
    }

    private fun initSession() {
        appResumedTimestamp = System.currentTimeMillis()
        val appStoppedTimestamp = sharedPrefsManager.getLastInteractionTime()
        val pausedTime = appResumedTimestamp - appStoppedTimestamp
        if (pausedTime > SESSION_RESET_TIME) {
            if (appStoppedTimestamp != 0L) {
                sessionEventFlow.tryEmit(
                    SessionEvent.SessionEndEvent(
                        sharedPrefsManager.getSessionId().orEmpty(),
                        System.currentTimeMillis(),
                        appStoppedTimestamp - sharedPrefsManager.getSessionStartTimestamp(),
                        sharedPrefsManager.getOpenCount(),
                        sharedPrefsManager.getBackgroundCount()
                    )
                )
            }
            sharedPrefsManager.saveOpenCount(0)
            sharedPrefsManager.saveBackgroundCount(0)
            previousForegroundTime = 0L
            sessionStartTimestamp = appResumedTimestamp
            sessionId = UUID.randomUUID().toString()
            sharedPrefsManager.saveSessionStartTimestamp(sessionStartTimestamp)
            sharedPrefsManager.saveSessionId(sessionId = sessionId)
            sessionEventFlow.tryEmit(SessionStartEvent(sessionId, sessionStartTimestamp))
        } else {
            previousForegroundTime = sharedPrefsManager.getAppSessionTime()
            sessionStartTimestamp = sharedPrefsManager.getSessionStartTimestamp()
            sessionId = sharedPrefsManager.getSessionId().orEmpty()
            if (sessionStartTimestamp == 0L) sessionStartTimestamp = appStoppedTimestamp
        }
        foregroundTimeMillis = previousForegroundTime
        sharedPrefsManager.saveOpenCount(sharedPrefsManager.getOpenCount() + 1)
    }

    override suspend fun clearSessionForced() {
        val appStoppedTimestamp = sharedPrefsManager.getLastInteractionTime()
        sessionEventFlow.emit(
            SessionEvent.SessionEndEvent(
                sharedPrefsManager.getSessionId().orEmpty(),
                System.currentTimeMillis(),
                appStoppedTimestamp - sharedPrefsManager.getSessionStartTimestamp(),
                sharedPrefsManager.getOpenCount(),
                sharedPrefsManager.getBackgroundCount()
            )
        )
        sharedPrefsManager.saveOpenCount(0)
        sharedPrefsManager.saveBackgroundCount(0)
        sharedPrefsManager.saveLastInteractionTime(0)
        previousForegroundTime = 0L
        foregroundTimeMillis = 0
    }

    private fun countTime() {
        timeSinceResume = System.currentTimeMillis() - appResumedTimestamp
        foregroundTimeMillis = previousForegroundTime + timeSinceResume
        //We need to save this values here because onPause may not be called on some devices if app removed from system tray
        sharedPrefsManager.saveAppSessionTime(foregroundTimeMillis)
        sharedPrefsManager.saveLastInteractionTime(System.currentTimeMillis())

        val nextMessages = closestScheduledMessages
        if (!nextMessages.isNullOrEmpty() && foregroundTimeMillis >= nextMessages.first().time) {
            scheduledMessages?.removeAll(nextMessages)
            findNextScheduledMessages()

            if (foregroundTimeMillis <= nextMessages.first().time + TIME_COUNTER_DELAY) { // check it is not too late to show inApp
                showInApp?.invoke(nextMessages.map { it.inApp })
            }
        }
    }

    companion object {
        private const val TIME_COUNTER_DELAY = 1000L
        private const val SESSION_RESET_TIME = 5L * 60L * 1000L
    }
}