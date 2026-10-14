package com.reteno.core.internal.action.iam

import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.domain.model.event.Event
import com.reteno.core.internal.event.EventBus
import com.reteno.core.internal.iam.InAppDisplayRules
import com.reteno.core.internal.iam.InAppMessagesState
import com.reteno.core.internal.log.LogPriority
import com.reteno.core.internal.log.Redact
import com.reteno.core.internal.log.RetenoLogSink
import com.reteno.core.internal.log.log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

internal interface InAppTriggering {
    fun listen()

    fun openEventGate()

    fun showDue(messages: List<InAppMessage>)
}

internal class InAppTriggers(
    private val eventBus: EventBus,
    private val state: InAppMessagesState,
    private val rules: InAppDisplayRules,
    private val showInApp: Lazy<ShowInApp>,
    private val sink: RetenoLogSink,
    private val scope: CoroutineScope
) : InAppTriggering {

    private val pendingEvents = Channel<Event>(MAX_PENDING_EVENTS, BufferOverflow.DROP_OLDEST)
    private val eventGate = CompletableDeferred<Unit>()
    private val listening = AtomicBoolean(false)

    override fun listen() {
        if (!listening.compareAndSet(false, true)) return
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            eventBus.events.collect { pendingEvents.trySend(it) }
        }
        scope.launch {
            eventGate.await()
            while (true) {
                onEventSafely(pendingEvents.receive())
            }
        }
    }

    override fun openEventGate() {
        eventGate.complete(Unit)
    }

    override fun showDue(messages: List<InAppMessage>) {
        scope.launch { showInApp.value(messages, showingOnAppStart = false) }
    }

    private suspend fun onEventSafely(event: Event) {
        try {
            onEvent(event)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            sink.log(LogPriority.ERROR, TAG) { "in-app event trigger failed: ${Redact.summary(error)}" }
        }
    }

    private suspend fun onEvent(event: Event) {
        val candidates = state.waitingForEvent()
            .filter { rules.matchesEvent(it.event, event) }
            .map { it.inApp }
        if (candidates.isEmpty()) return
        showInApp.value(candidates, showingOnAppStart = false)
    }

    companion object {
        const val MAX_PENDING_EVENTS = 50
        private const val TAG = "Reteno"
    }
}
