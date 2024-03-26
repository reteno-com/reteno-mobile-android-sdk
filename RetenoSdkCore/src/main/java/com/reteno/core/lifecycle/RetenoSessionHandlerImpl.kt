package com.reteno.core.lifecycle

import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.data.remote.model.iam.displayrules.targeting.InAppWithTime
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class RetenoSessionHandlerImpl(private val sharedPrefsManager: SharedPrefsManager): RetenoSessionHandler {

    private var foregroundTimeMillis: Long = 0L
    private var sessionStartTimestamp: Long = 0L

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
        appPausedTimestamp = System.currentTimeMillis()
        sharedPrefsManager.saveAppSessionTime(foregroundTimeMillis)
        sharedPrefsManager.saveAppStoppedTimestamp(appPausedTimestamp)
        job?.cancel()
        job = null
    }

    override fun scheduleInAppMessages(messages: MutableList<InAppWithTime>, onTimeMatch: (List<InAppMessage>) -> Unit) {
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
        val appStoppedTimestamp = sharedPrefsManager.getAppStoppedTimestamp()
        val pausedTime = appResumedTimestamp - appStoppedTimestamp
        if (pausedTime > SESSION_RESET_TIME) {
            previousForegroundTime = 0L
            sessionStartTimestamp = appResumedTimestamp
            sharedPrefsManager.saveSessionStartTimestamp(sessionStartTimestamp)
        } else {
            previousForegroundTime = sharedPrefsManager.getAppSessionTime()
            sessionStartTimestamp = sharedPrefsManager.getSessionStartTimestamp()
            if (sessionStartTimestamp == 0L) sessionStartTimestamp = appStoppedTimestamp
        }
        foregroundTimeMillis = previousForegroundTime
    }

    private fun countTime() {
        timeSinceResume = System.currentTimeMillis() - appResumedTimestamp
        foregroundTimeMillis = previousForegroundTime + timeSinceResume

        val nextMessages = closestScheduledMessages
        if (nextMessages != null && nextMessages.isNotEmpty() && foregroundTimeMillis >= nextMessages.first().time) {
            scheduledMessages?.removeAll(nextMessages)
            findNextScheduledMessages()

            if (nextMessages.first().time <= foregroundTimeMillis + TIME_COUNTER_DELAY) { // check it is not too late to show inApp
                showInApp?.invoke(nextMessages.map { it.inApp })
            }
        }
    }

    companion object {
        private const val TIME_COUNTER_DELAY = 1000L
        private const val SESSION_RESET_TIME = 5L * 60L * 1000L
    }
}