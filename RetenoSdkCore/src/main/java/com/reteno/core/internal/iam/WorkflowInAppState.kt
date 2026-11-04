package com.reteno.core.internal.iam

import com.google.gson.JsonObject
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppInteraction
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.network.SuspendApiClient
import kotlinx.coroutines.withContext
import java.time.Instant

internal class WorkflowInAppState(
    private val api: SuspendApiClient,
    private val interactions: RetenoDatabaseManagerInAppInteraction,
    private val dispatchers: RetenoDispatchers,
    private val clock: () -> Long
) {

    private val lock = Any()
    private var latest: List<InAppMessage> = emptyList()
    private val shown = mutableSetOf<Pair<String?, String?>>()

    fun context(): String = api.workflowContext()

    fun isCurrent(message: InAppMessage): Boolean = message.workflowContext == context()

    fun remember(messages: List<InAppMessage>) {
        synchronized(lock) {
            latest = messages.filter { it.inWorkflow }
            val retained = latest.map { it.workflowContext to it.iid }.toSet()
            shown.retainAll(retained)
        }
    }

    fun markShown(message: InAppMessage) {
        synchronized(lock) { shown.add(message.workflowContext to message.iid) }
    }

    fun canPresent(message: InAppMessage): Boolean {
        if (!isCurrent(message) || message.iid.isNullOrBlank()) return false
        val unexpired = runCatching {
            Instant.ofEpochMilli(clock()).isBefore(Instant.parse(message.expiresAt))
        }.getOrDefault(false)
        if (!unexpired) return false
        return synchronized(lock) {
            (message.workflowContext to message.iid) !in shown && matchesCurrentRules(message)
        }
    }

    suspend fun isEligible(message: InAppMessage): Boolean {
        if (!canPresent(message)) return false
        val persisted = withContext(dispatchers.io) {
            interactions.isWorkflowShown(requireNotNull(message.workflowContext), requireNotNull(message.iid))
        }
        if (persisted) markShown(message)
        return !persisted && canPresent(message)
    }

    private fun matchesCurrentRules(message: InAppMessage): Boolean {
        val current = latest.firstOrNull { it.workflowContext == message.workflowContext && it.iid == message.iid }
            ?: return true
        return rulesWithoutFrequency(current.displayRulesJson) == rulesWithoutFrequency(message.displayRulesJson)
    }

    private fun rulesWithoutFrequency(rules: JsonObject): JsonObject = rules.deepCopy().apply { remove("FREQUENCY") }
}
