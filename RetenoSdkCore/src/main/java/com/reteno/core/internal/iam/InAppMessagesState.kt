package com.reteno.core.internal.iam

import com.reteno.core.data.remote.model.iam.displayrules.targeting.InAppWithEvent
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.features.iam.InAppPauseBehaviour

internal interface InAppMessagesState {
    val isPaused: Boolean

    fun setPaused(paused: Boolean): InAppMessage?

    fun setPauseBehaviour(behaviour: InAppPauseBehaviour)

    fun postponeIfPaused(message: InAppMessage): Boolean

    fun replaceMessages(messages: List<InAppMessage>)

    fun messages(): List<InAppMessage>

    fun replaceWaitingForEvent(inApps: List<InAppWithEvent>)

    fun waitingForEvent(): List<InAppWithEvent>
}

internal class InAppMessagesStateImpl : InAppMessagesState {

    private val lock = Any()
    private var paused = false
    private var pauseBehaviour = InAppPauseBehaviour.POSTPONE_IN_APPS
    private val postponed = mutableListOf<InAppMessage>()
    private var messages: List<InAppMessage> = emptyList()
    private var waitingForEvent: List<InAppWithEvent> = emptyList()

    override val isPaused: Boolean
        get() = synchronized(lock) { paused }

    override fun setPaused(paused: Boolean): InAppMessage? = synchronized(lock) {
        val resumed = this.paused && !paused
        this.paused = paused
        if (!resumed) return null
        val toShow = postponed.firstOrNull().takeIf { pauseBehaviour == InAppPauseBehaviour.POSTPONE_IN_APPS }
        postponed.clear()
        toShow
    }

    override fun setPauseBehaviour(behaviour: InAppPauseBehaviour) {
        synchronized(lock) { pauseBehaviour = behaviour }
    }

    override fun postponeIfPaused(message: InAppMessage): Boolean = synchronized(lock) {
        if (paused) postponed += message
        paused
    }

    override fun replaceMessages(messages: List<InAppMessage>) {
        synchronized(lock) { this.messages = messages.toList() }
    }

    override fun messages(): List<InAppMessage> = synchronized(lock) { messages }

    override fun replaceWaitingForEvent(inApps: List<InAppWithEvent>) {
        synchronized(lock) { waitingForEvent = inApps.toList() }
    }

    override fun waitingForEvent(): List<InAppWithEvent> = synchronized(lock) { waitingForEvent }
}
