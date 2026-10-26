package com.reteno.core.internal.action.sync

import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.action.trace.TracedOutcome
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal enum class SyncScope {
    CONTACT,
    ALL
}

internal enum class SyncResult(override val isSuccessful: Boolean) : TracedOutcome {
    COMPLETED(true),
    INCOMPLETE(false),
    ABORTED(false)
}

internal class SyncDataAction(
    private val contactSteps: List<SyncStep>,
    private val dataSteps: List<SyncStep>,
    private val runner: ActionRunner,
    private val stepLock: Mutex = Mutex()
) {

    suspend operator fun invoke(scope: SyncScope): Result<SyncResult> = sync(scope).map(SyncRun::result)

    private suspend fun sync(scope: SyncScope): Result<SyncRun> = runner("Sync", collapseOnSuccess = true) {
        args("scope" to scope)
        val steps = if (scope == SyncScope.CONTACT) contactSteps else contactSteps + dataSteps
        runSteps(steps)
    }

    private suspend fun ActionTrace.runSteps(steps: List<SyncStep>): SyncRun {
        val sent = mutableListOf<String>()
        var result = SyncResult.COMPLETED
        steps.forEachIndexed { index, syncStep ->
            val outcome = stepLock.withLock { syncStep() }
            val position = "step ${index + 1} of ${steps.size}"
            when {
                outcome.isFailure && index < contactSteps.size -> {
                    note { "$position failed, contact data must be sent first" }
                    return SyncRun(SyncResult.ABORTED, sent)
                }
                outcome.isFailure -> {
                    note { "$position failed" }
                    result = SyncResult.INCOMPLETE
                }
                outcome.getOrThrow() == StepOutcome.ABORT -> {
                    note { "stopped after $position" }
                    return SyncRun(SyncResult.ABORTED, sent)
                }
                outcome.getOrThrow() == StepOutcome.CONTINUE -> {
                    sent += syncStep.name
                }
            }
        }
        return SyncRun(result, sent)
    }

    private class SyncRun(val result: SyncResult, private val sent: List<String>) : TracedOutcome {

        override val isSuccessful: Boolean
            get() = result.isSuccessful

        override val traceLabel: String
            get() = when {
                sent.isNotEmpty() -> "$result  sent=${sent.joinToString()}"
                result == SyncResult.COMPLETED -> "$result  nothing to send"
                else -> "$result"
            }
    }
}
