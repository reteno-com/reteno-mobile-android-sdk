package com.reteno.core.domain.model.event

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
        internal fun sessionStart(
            sessionId: String,
            startTime: ZonedDateTime
        ) = Custom(
            SESSION_START_EVENT_TYPE_KEY,
            startTime,
            listOf(Parameter(SESSION_ID_PARAM_NAME,  sessionId))
        )
        internal const val SCREEN_VIEW_EVENT_TYPE_KEY = "screenView"
        internal const val SCREEN_VIEW_PARAM_NAME = "screenClass"
        internal const val SESSION_START_EVENT_TYPE_KEY = "SessionStarted"
        internal const val SESSION_ID_PARAM_NAME = "sessionID"
    }
}