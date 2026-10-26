package com.reteno.core.internal.lifecycle

import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.data.remote.model.iam.displayrules.targeting.InAppWithTime
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.event.LifecycleEvent
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.internal.action.event.LogEvent
import com.reteno.core.lifecycle.RetenoSessionHandler
import com.reteno.core.util.Util.asZonedDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal class SessionTracker(
    private val prefs: SharedPrefsManager,
    private val filter: LifecycleEventFilter,
    private val logEvent: LogEvent,
    private val scope: CoroutineScope,
    private val clock: () -> Long,
    private val newSessionId: () -> String
) : RetenoSessionHandler {

    private var sessionResetTime = DEFAULT_SESSION_RESET_TIME
    private var foregroundTimeMillis = 0L
    private var previousForegroundTime = 0L
    private var appResumedTimestamp = 0L
    private var sessionStartTimestamp = 0L
    private var sessionId = ""
    private var scheduledMessages: MutableList<InAppWithTime> = mutableListOf()
    private var closestScheduledMessages: List<InAppWithTime> = emptyList()
    private var showInApp: ((List<InAppMessage>) -> Unit)? = null
    private var job: Job? = null

    @Synchronized
    override fun start() {
        if (job != null) return
        onAppForegrounded()
        job = scope.launch {
            while (true) {
                tick().forEach { logEvent(it) }
                delay(TICK_MILLIS)
            }
        }
    }

    @Synchronized
    override fun stop() {
        prefs.saveBackgroundCount(prefs.getBackgroundCount() + 1)
        prefs.saveAppSessionTime(foregroundTimeMillis)
        prefs.saveAppStoppedTimestamp(clock())
        job?.cancel()
        job = null
    }

    override fun setSessionResetDuration(duration: Long) {
        sessionResetTime = duration
    }

    @Synchronized
    override fun scheduleInAppMessages(
        messages: MutableList<InAppWithTime>,
        onTimeMatch: (List<InAppMessage>) -> Unit
    ) {
        if (messages.isEmpty()) return
        messages.sortBy { it.time }
        showInApp = onTimeMatch
        scheduledMessages = messages
        closestScheduledMessages = nextScheduled(messages)
    }

    override fun getForegroundTimeMillis(): Long = foregroundTimeMillis

    override fun getSessionStartTimestamp(): Long = sessionStartTimestamp

    override fun getSessionId(): String = sessionId

    override fun setLifecycleEventConfig(lifecycleEventConfig: LifecycleTrackingOptions) {
        filter.update(lifecycleEventConfig)
    }

    @Synchronized
    override fun clearSessionForced() {
        prefs.batchSave(openCount = 0, backgroundCount = 0, lastInteractionTime = 0)
        previousForegroundTime = 0L
        foregroundTimeMillis = 0L
    }

    private fun onAppForegrounded() {
        appResumedTimestamp = clock()
        previousForegroundTime = prefs.getAppSessionTime()
        foregroundTimeMillis = previousForegroundTime
        sessionId = prefs.getSessionId().orEmpty()
        val storedStart = prefs.getSessionStartTimestamp()
        sessionStartTimestamp = if (storedStart == 0L) prefs.getLastInteractionTime() else storedStart
        prefs.saveOpenCount(prefs.getOpenCount() + 1)
    }

    @Synchronized
    private fun tick(): List<Event> {
        val sessionEvents = if (clock() - sessionStartTimestamp > sessionResetTime) startSession() else emptyList()
        countForegroundTime()
        closestScheduledMessages = nextScheduled(scheduledMessages)
        return sessionEvents.filter { filter.isEnabled(it.type) }.map { it.event }
    }

    private fun startSession(): List<LifecycleEvent> {
        val now = clock()
        val previousSessionId = prefs.getSessionId().orEmpty()
        val ended = if (previousSessionId.isEmpty()) {
            null
        } else {
            Event.sessionEnd(
                previousSessionId,
                now.asZonedDateTime(),
                (now - prefs.getSessionStartTimestamp()).milliseconds.inWholeSeconds.toInt(),
                prefs.getOpenCount(),
                prefs.getBackgroundCount()
            )
        }
        prefs.saveOpenCount(0)
        prefs.saveBackgroundCount(0)
        previousForegroundTime = 0L
        sessionStartTimestamp = now
        sessionId = newSessionId()
        prefs.saveSessionStartTimestamp(sessionStartTimestamp)
        prefs.saveSessionId(sessionId)
        return listOfNotNull(ended, Event.sessionStart(sessionId, sessionStartTimestamp.asZonedDateTime()))
    }

    private fun countForegroundTime() {
        val now = clock()
        foregroundTimeMillis = previousForegroundTime + (now - appResumedTimestamp)
        prefs.batchSaveSession(sessionTime = foregroundTimeMillis, lastInteractionTime = now)
        val show = showInApp ?: return
        val due = closestScheduledMessages
        if (due.isEmpty() || foregroundTimeMillis < due.first().time) return
        scheduledMessages.removeAll(due)
        if (foregroundTimeMillis <= due.first().time + TICK_MILLIS) {
            show(due.map { it.inApp })
        }
    }

    private fun nextScheduled(messages: List<InAppWithTime>): List<InAppWithTime> {
        val next = messages.firstOrNull() ?: return emptyList()
        return messages.filter { it.time == next.time }
    }

    companion object {
        private const val TICK_MILLIS = 1_000L
        internal const val DEFAULT_SESSION_RESET_TIME = 3 * 60L * 60L * 1000L
    }
}
