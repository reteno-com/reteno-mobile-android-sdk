package com.reteno.core.internal.action.iam

import com.reteno.core.data.remote.model.iam.displayrules.targeting.InAppWithEvent
import com.reteno.core.data.remote.model.iam.displayrules.targeting.InAppWithTime
import com.reteno.core.data.remote.model.iam.displayrules.targeting.TargetingRule
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.iam.InAppMessagesState
import com.reteno.core.lifecycle.RetenoSessionHandler
import kotlinx.coroutines.CancellationException

internal interface ProcessInAppMessages {
    suspend operator fun invoke(messages: List<InAppMessage>): Result<Unit>
}

internal class ProcessInAppMessagesAction(
    private val state: InAppMessagesState,
    private val sessionHandler: RetenoSessionHandler,
    private val triggers: InAppTriggering,
    private val showInApp: ShowInApp,
    private val runner: ActionRunner
) : ProcessInAppMessages {

    override suspend operator fun invoke(
        messages: List<InAppMessage>
    ): Result<Unit> = runner("ProcessInAppMessages") {
        args("messages" to messages.size)
        try {
            process(messages)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            triggers.openEventGate()
            throw error
        }
    }

    private suspend fun ActionTrace.process(messages: List<InAppMessage>) {
        val sorted = step<SortedInApps>(
            "sort in-app messages by trigger",
            detail = { "onAppStart=${it.onAppStart.size}  events=${it.byEvent.size}  timers=${it.byTime.size}" }
        ) {
            sort(messages)
        }
        if (sorted.byTime.isNotEmpty()) {
            step("schedule time spent in-apps in session") {
                sessionHandler.scheduleInAppMessages(sorted.byTime.toMutableList()) { due -> triggers.showDue(due) }
            }
        }
        step("replace in-apps waiting for events in in-app state") {
            state.replaceWaitingForEvent(sorted.byEvent)
        }
        step("open in-app event gate") {
            triggers.openEventGate()
        }
        showInApp(sorted.onAppStart, showingOnAppStart = true)
    }

    private fun sort(messages: List<InAppMessage>): SortedInApps {
        val byTime = mutableListOf<InAppWithTime>()
        val byEvent = mutableListOf<InAppWithEvent>()
        val onAppStart = mutableListOf<InAppMessage>()
        messages.forEach { message ->
            val include = message.displayRules.targeting?.include
            if (include == null) {
                onAppStart += message
                return@forEach
            }
            include.groups.flatMap { it.conditions }.forEach { rule ->
                if (rule is TargetingRule.Event) {
                    byEvent += InAppWithEvent(message, rule)
                } else {
                    byTime += InAppWithTime(message, (rule as TargetingRule.TimeSpentInApp).timeSpentMillis)
                }
            }
        }
        return SortedInApps(byTime, byEvent, onAppStart)
    }

    private class SortedInApps(
        val byTime: List<InAppWithTime>,
        val byEvent: List<InAppWithEvent>,
        val onAppStart: List<InAppMessage>
    )
}
