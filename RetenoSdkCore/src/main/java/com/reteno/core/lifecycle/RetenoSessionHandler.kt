package com.reteno.core.lifecycle

import com.reteno.core.data.remote.model.iam.displayrules.targeting.InAppWithTime
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import kotlinx.coroutines.flow.Flow

interface RetenoSessionHandler {

    val sessionEventFlow: Flow<SessionEvent>

    fun getForegroundTimeMillis(): Long
    fun getSessionStartTimestamp(): Long
    fun getSessionId(): String
    fun start()
    fun stop()
    fun scheduleInAppMessages(
        messages: MutableList<InAppWithTime>,
        onTimeMatch: (List<InAppMessage>) -> Unit
    )


    sealed interface SessionEvent {
        class SessionEndEvent(
            val sessionId: String,
            val endTime: Long,
            val durationInMillis: Long,
            val openCount: Int,
            val bgCount: Int
        ) : SessionEvent

        class SessionStartEvent(
            val sessionId: String,
            val startTime: Long,
        ) : SessionEvent
    }

}