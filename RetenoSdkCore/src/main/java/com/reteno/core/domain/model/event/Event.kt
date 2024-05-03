package com.reteno.core.domain.model.event

import com.reteno.core.util.Util.asZonedDateTime
import com.reteno.core.util.Util.formatToRemote
import java.time.ZonedDateTime

sealed class Event(
    val eventTypeKey: String,
    val occurred: ZonedDateTime,
    val params: List<Parameter>? = null
) {

    data class Custom(
        val typeKey: String,
        val dateOccurred: ZonedDateTime,
        val parameters: List<Parameter>? = null
    ) : Event(typeKey, dateOccurred, parameters)

    data class ScreenView(val screenName: String) : Event(
        SCREEN_VIEW_EVENT_TYPE_KEY,
        ZonedDateTime.now(),
        listOf(Parameter(SCREEN_VIEW_PARAM_NAME, screenName))
    )

    companion object {

        internal fun applicationInstall(
            version: String,
            build: Long
        ) = LifecycleEvent(
            LifecycleEventType.APP_LIFECYCLE,
            Custom(
                typeKey = LIFECYCLE_EVENT_APP_INSTALLED,
                dateOccurred = ZonedDateTime.now(),
                listOf(
                    Parameter(APP_VERSION_PARAM_NAME, version),
                    Parameter(APP_BUILD_PARAM_NAME, build.toString())
                )
            )
        )

        internal fun applicationUpdate(
            version: String,
            build: Long,
            prevVersion: String,
            prevBuild: Long
        ) = LifecycleEvent(
            LifecycleEventType.APP_LIFECYCLE,
            Custom(
                typeKey = LIFECYCLE_EVENT_APP_UPDATED,
                dateOccurred = ZonedDateTime.now(),
                listOf(
                    Parameter(APP_VERSION_PARAM_NAME, version),
                    Parameter(APP_BUILD_PARAM_NAME, build.toString()),
                    Parameter(PREV_VERSION_PARAM_NAME, prevVersion),
                    Parameter(PREV_BUILD_PARAM_NAME, prevBuild.toString())
                )
            )
        )

        internal fun applicationOpen(
            fromBackground: Boolean
        ) = LifecycleEvent(
            LifecycleEventType.APP_LIFECYCLE,
            Custom(
                typeKey = LIFECYCLE_EVENT_APP_OPENED,
                dateOccurred = ZonedDateTime.now(),
                listOf(
                    Parameter(FROM_BACKGROUND_PARAM_NAME, fromBackground.toString())
                )
            )
        )

        internal fun applicationBackgrounded(
            applicationOpenedTime: Long,
            secondsInForeground: Long
        ) = LifecycleEvent(
            LifecycleEventType.APP_LIFECYCLE,
            Custom(
                typeKey = LIFECYCLE_EVENT_APP_BACKGROUNDED,
                dateOccurred = ZonedDateTime.now(),
                listOf(
                    Parameter(
                        APPLICATION_OPENED_TIME_PARAM_NAME,
                        applicationOpenedTime.asZonedDateTime().formatToRemote()
                    ),
                    Parameter(SECONDS_IN_FOREGROUND_PARAM_NAME, secondsInForeground.toString())
                )
            )
        )

        internal fun notificationsEnabled() = LifecycleEvent(
            LifecycleEventType.PUSH,
            Custom(
                typeKey = LIFECYCLE_EVENT_PUSH_SUBSCRIBED,
                dateOccurred = ZonedDateTime.now(),
            )
        )

        internal fun notificationsDisabled() = LifecycleEvent(
            LifecycleEventType.PUSH,
            Custom(
                typeKey = LIFECYCLE_EVENT_PUSH_UNSUBSCRIBED,
                dateOccurred = ZonedDateTime.now(),
            )
        )

        internal fun sessionStart(
            sessionId: String,
            startTime: ZonedDateTime
        ) = LifecycleEvent(
            LifecycleEventType.SESSION,
            Custom(
                SESSION_START_EVENT_TYPE_KEY,
                startTime,
                listOf(
                    Parameter(SESSION_ID_PARAM_NAME, sessionId),
                    Parameter(SESSION_START_TIME_PARAM_NAME, startTime.formatToRemote())
                )
            )
        )

        internal fun sessionEnd(
            sessionId: String,
            endTime: ZonedDateTime,
            durationInSeconds: Int,
            openCount: Int,
            bgCount: Int
        ) = LifecycleEvent(
            LifecycleEventType.SESSION,
            Custom(
                SESSION_END_EVENT_TYPE_KEY,
                ZonedDateTime.now(),
                listOf(
                    Parameter(SESSION_ID_PARAM_NAME, sessionId),
                    Parameter(END_TIME_PARAM_NAME, endTime.formatToRemote()),
                    Parameter(DURATION_IN_SECONDS_PARAM_NAME, durationInSeconds.toString()),
                    Parameter(OPENED_COUNT_PARAM_NAME, openCount.toString()),
                    Parameter(BG_COUNT_PARAM_NAME, bgCount.toString())
                ),
            )
        )

        internal const val SCREEN_VIEW_EVENT_TYPE_KEY = "screenView"
        internal const val SCREEN_VIEW_PARAM_NAME = "screenClass"
        internal const val SESSION_START_TIME_PARAM_NAME = "startTime"
        internal const val SESSION_START_EVENT_TYPE_KEY = "SessionStarted"
        internal const val SESSION_END_EVENT_TYPE_KEY = "SessionEnded"
        internal const val END_TIME_PARAM_NAME = "endTime"
        internal const val DURATION_IN_SECONDS_PARAM_NAME = "durationInSeconds"
        internal const val OPENED_COUNT_PARAM_NAME = "applicationOpenedCount"
        internal const val BG_COUNT_PARAM_NAME = "applicationBackgroundedCount"
        internal const val SESSION_ID_PARAM_NAME = "sessionID"
        internal const val LIFECYCLE_EVENT_APP_INSTALLED = "ApplicationInstalled"
        internal const val APP_VERSION_PARAM_NAME = "version"
        internal const val PREV_BUILD_PARAM_NAME = "previousBuild"
        internal const val APP_BUILD_PARAM_NAME = "build"
        internal const val LIFECYCLE_EVENT_APP_UPDATED = "ApplicationUpdated"
        internal const val PREV_VERSION_PARAM_NAME = "previousVersion"
        internal const val LIFECYCLE_EVENT_APP_OPENED = "ApplicationOpened"
        internal const val FROM_BACKGROUND_PARAM_NAME = "fromBackground"
        internal const val LIFECYCLE_EVENT_APP_BACKGROUNDED = "ApplicationBackgrounded"
        internal const val APPLICATION_OPENED_TIME_PARAM_NAME = "applicationOpenedTime"
        internal const val SECONDS_IN_FOREGROUND_PARAM_NAME = "secondsInForeground"
        internal const val LIFECYCLE_EVENT_PUSH_SUBSCRIBED = "PushNotificationsSubscribed"
        internal const val LIFECYCLE_EVENT_PUSH_UNSUBSCRIBED = "PushNotificationsUnsubscribed"
    }
}