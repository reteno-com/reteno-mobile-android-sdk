package com.reteno.core.internal.action.iam

import com.reteno.core.data.remote.model.iam.displayrules.frequency.FrequencyRule
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.internal.action.trace.ActionRunner
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal interface RecordInAppShown {
    suspend operator fun invoke(message: InAppMessage): Result<Unit>
}

internal class RecordInAppShownAction(
    private val cache: InAppMessagesCache,
    private val clock: () -> Long,
    private val lock: Mutex,
    private val runner: ActionRunner
) : RecordInAppShown {

    override suspend operator fun invoke(message: InAppMessage): Result<Unit> = runner("RecordInAppShown") {
        args("messageId" to message.messageId)
        lock.withLock {
            step<Long>("count show in in-app message", detail = { "count=$it" }) {
                countShow(message)
            }
            step("save show history to local database") {
                cache.update(listOf(message))
            }
        }
    }

    private fun countShow(message: InAppMessage): Long {
        val now = clock()
        message.showCount = if (restartsCount(message, now)) 1 else message.showCount + 1
        message.lastShowTime = now
        return message.showCount
    }

    private fun restartsCount(message: InAppMessage, now: Long): Boolean {
        val frequency = message.displayRules.frequency ?: return false
        val longestUnit = frequency.predicates
            .filterIsInstance<FrequencyRule.TimesPerTimeUnit>()
            .maxOfOrNull { it.timeUnit.toMillis(1) } ?: return false
        val lastShowTime = message.lastShowTime ?: return false
        return now - lastShowTime > longestUnit
    }
}
