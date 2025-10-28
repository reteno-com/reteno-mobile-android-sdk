package com.reteno.core.lifecycle

import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.data.remote.model.iam.displayrules.targeting.InAppWithTime
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.domain.controller.EventController
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.event.LifecycleEvent
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.util.Logger
import com.reteno.core.util.Util.asZonedDateTime
import com.reteno.core.util.Util.toTypeMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

internal class RetenoSessionHandlerImpl(
    private val eventController: EventController,
    private val sharedPrefsManager: SharedPrefsManager,
    lifecycleTrackingOptions: LifecycleTrackingOptions,
) : RetenoSessionHandler {

    private var lifecycleEventConfig = lifecycleTrackingOptions.toTypeMap()
    private var foregroundTimeMillis: Long = 0L
    private var sessionStartTimestamp: Long = 0L
    private var sessionId = ""

    private var timeSinceResume: Long = 0L
    private var appResumedTimestamp: Long = 0L

    private var previousForegroundTime: Long = 0L
    private var appPausedTimestamp: Long = -1L

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var scheduledMessages: MutableList<InAppWithTime> = mutableListOf()
    private var closestScheduledMessages: List<InAppWithTime> = emptyList()
    private var showInApp: ((List<InAppMessage>) -> Unit)? = null

    override fun start() {
        initSession()
        job = scope.launch {
            while (true) {
                synchronized(this@RetenoSessionHandlerImpl) {
                    countTime(scheduledMessages, closestScheduledMessages)
                    closestScheduledMessages = findNextScheduledMessages(scheduledMessages)
                }
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
        messages.sortBy { it.time }
        synchronized(this) {
            showInApp = onTimeMatch
            scheduledMessages = messages
            closestScheduledMessages = findNextScheduledMessages(messages)
        }
    }

    override fun getForegroundTimeMillis(): Long {
        return foregroundTimeMillis
    }

    override fun getSessionStartTimestamp(): Long {
        return sessionStartTimestamp
    }

    override fun setLifecycleEventConfig(lifecycleEventConfig: LifecycleTrackingOptions) {
        /*@formatter:off*/ Logger.i(TAG, "setLifecycleEventConfig(): ", "lifecycleEventConfig = [" , lifecycleEventConfig , "]")
        /*@formatter:on*/
        this.lifecycleEventConfig = lifecycleEventConfig.toTypeMap()
    }

    override fun getSessionId(): String {
        return sessionId
    }

    private fun findNextScheduledMessages(messages: List<InAppWithTime>): List<InAppWithTime> {
        if (messages.isEmpty()) return emptyList()

        val nextMessageTime = messages.first().time
        return messages.filter { it.time == nextMessageTime }
    }

    private fun initSession() {
        appResumedTimestamp = System.currentTimeMillis()
        val appStoppedTimestamp = sharedPrefsManager.getLastInteractionTime()
        val pausedTime = appResumedTimestamp - appStoppedTimestamp
        if (pausedTime > SESSION_RESET_TIME) {
            if (appStoppedTimestamp != 0L) {
                trackSessionEvent(
                    Event.sessionEnd(
                        sharedPrefsManager.getSessionId().orEmpty(),
                        System.currentTimeMillis().asZonedDateTime(),
                        (appStoppedTimestamp - sharedPrefsManager.getSessionStartTimestamp()).milliseconds.inWholeSeconds.toInt(),
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
            trackSessionEvent(
                Event.sessionStart(
                    sessionId,
                    sessionStartTimestamp.asZonedDateTime()
                )
            )
        } else {
            previousForegroundTime = sharedPrefsManager.getAppSessionTime()
            sessionStartTimestamp = sharedPrefsManager.getSessionStartTimestamp()
            sessionId = sharedPrefsManager.getSessionId().orEmpty()
            if (sessionStartTimestamp == 0L) sessionStartTimestamp = appStoppedTimestamp
        }
        foregroundTimeMillis = previousForegroundTime
        sharedPrefsManager.saveOpenCount(sharedPrefsManager.getOpenCount() + 1)
    }

    override fun clearSessionForced() {
        val appStoppedTimestamp = sharedPrefsManager.getLastInteractionTime()
        trackSessionEvent(
            Event.sessionEnd(
                sharedPrefsManager.getSessionId().orEmpty(),
                System.currentTimeMillis().asZonedDateTime(),
                (appStoppedTimestamp - sharedPrefsManager.getSessionStartTimestamp()).milliseconds.inWholeSeconds.toInt(),
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

    private fun trackSessionEvent(lifecycleEvent: LifecycleEvent) {
        if (lifecycleEventConfig.getOrElse(lifecycleEvent.type) { false }) {
            eventController.trackEvent(lifecycleEvent.event)
        }
    }

    private fun countTime(
        scheduled: MutableList<InAppWithTime>,
        closest: List<InAppWithTime>
    ) {
        timeSinceResume = System.currentTimeMillis() - appResumedTimestamp
        foregroundTimeMillis = previousForegroundTime + timeSinceResume
        //We need to save this values here because onPause may not be called on some devices if app removed from system tray
        sharedPrefsManager.saveAppSessionTime(foregroundTimeMillis)
        sharedPrefsManager.saveLastInteractionTime(System.currentTimeMillis())

        if (closest.isNotEmpty() && foregroundTimeMillis >= closest.first().time) {
            scheduled.removeAll(closest)

            if (foregroundTimeMillis <= closest.first().time + TIME_COUNTER_DELAY) { // check it is not too late to show inApp
                showInApp?.invoke(closest.map { it.inApp })
            }
        }
    }

    companion object {
        private val TAG = RetenoSessionHandlerImpl::class.java.simpleName
        private const val TIME_COUNTER_DELAY = 1000L
        private const val SESSION_RESET_TIME = 5L * 60L * 1000L
    }
}