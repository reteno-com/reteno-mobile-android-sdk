package com.reteno.core.internal.action.iam

import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.action.trace.TracedOutcome
import com.reteno.core.internal.iam.DisplayDecision
import com.reteno.core.internal.iam.InAppDisplayRules
import com.reteno.core.internal.iam.InAppMessagesState
import com.reteno.core.internal.iam.DeliveryInAppState
import com.reteno.core.lifecycle.RetenoSessionHandler
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal interface InAppDisplay {
    suspend fun show(
        message: InAppMessage,
        owner: String? = null,
        onRejected: (suspend () -> Unit)? = null
    ): DisplayOffer
}

internal enum class DisplayOffer(override val traceLabel: String) : TracedOutcome {
    LOADING("loading"),
    WAITING_FOR_APP("waiting for the app to come to foreground"),
    SKIPPED("skipped, another in-app message is on screen");

    override val isSuccessful: Boolean
        get() = true
}

internal interface ShowInApp {
    suspend operator fun invoke(candidates: List<InAppMessage>, showingOnAppStart: Boolean): Result<Boolean>
}

internal class ShowInAppAction(
    private val rules: InAppDisplayRules,
    private val state: InAppMessagesState,
    private val checkSegments: CheckInAppSegments,
    private val sessionHandler: RetenoSessionHandler,
    private val display: InAppDisplay,
    private val lock: Mutex,
    private val runner: ActionRunner,
    private val delivery: DeliveryInAppState
) : ShowInApp {

    override suspend operator fun invoke(
        candidates: List<InAppMessage>,
        showingOnAppStart: Boolean
    ): Result<Boolean> = runner("ShowInApp") {
        args("candidates" to candidates.size, "onAppStart" to showingOnAppStart)
        show(candidates, showingOnAppStart)
    }

    private suspend fun ActionTrace.show(candidates: List<InAppMessage>, showingOnAppStart: Boolean): Boolean {
        val owner = delivery.context()
        if (candidates.isEmpty()) {
            note { "no in-app message to show" }
            return false
        }
        if (!showingOnAppStart) {
            checkSegments(candidates, persist = true)
        }
        return lock.withLock { offerBest(candidates, showingOnAppStart, owner) }
    }

    private suspend fun ActionTrace.offerBest(
        candidates: List<InAppMessage>,
        showingOnAppStart: Boolean,
        owner: String
    ): Boolean {
        if (owner != delivery.context()) return false
        val chosen = choose(candidates, showingOnAppStart)
        if (chosen == null) {
            note { "no in-app message passed display rules" }
            return false
        }
        if (owner != delivery.context()) return false
        val postponed = step<Boolean>(
            "postpone in-app message in in-app state while paused",
            detail = { if (it) "postponed ${chosen.messageId}" else "not paused" }
        ) {
            state.postponeIfPaused(chosen) {
                if (owner == delivery.context()) {
                    invoke(candidates, showingOnAppStart)
                }
            }
        }
        if (postponed) return true
        step("pass in-app message to display") {
            display.show(chosen, owner) {
                if (owner == delivery.context()) {
                    invoke(candidates.filterNot { it === chosen }, showingOnAppStart)
                }
            }
        }
        return true
    }

    private suspend fun ActionTrace.choose(candidates: List<InAppMessage>, showingOnAppStart: Boolean): InAppMessage? {
        val sessionStart = sessionHandler.getSessionStartTimestamp()
        val ordered = candidates.sortedWith(
            compareByDescending<InAppMessage> { it.priority }.thenByDescending { it.messageId }
        )
        return ordered.firstOrNull { message ->
            if (message.serverAssignedIid && !delivery.isEligible(message)) return@firstOrNull false
            val decision = rules.decide(message, sessionStart, showingOnAppStart)
            note { "message ${message.messageId}: $decision" }
            decision == DisplayDecision.SHOW
        }
    }
}
