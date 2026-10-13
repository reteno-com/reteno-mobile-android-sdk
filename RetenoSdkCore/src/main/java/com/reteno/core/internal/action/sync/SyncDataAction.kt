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

    suspend operator fun invoke(scope: SyncScope): Result<SyncResult> = runner("Sync") {
        args("scope" to scope)
        val steps = if (scope == SyncScope.CONTACT) contactSteps else contactSteps + dataSteps
        runSteps(steps)
    }

    private suspend fun ActionTrace.runSteps(steps: List<SyncStep>): SyncResult {
        var result = SyncResult.COMPLETED
        steps.forEachIndexed { index, step ->
            val outcome = stepLock.withLock { step() }
            val position = "step ${index + 1} of ${steps.size}"
            when {
                outcome.isFailure && index < contactSteps.size -> {
                    note { "$position failed, contact data must be sent first" }
                    return SyncResult.ABORTED
                }
                outcome.isFailure -> {
                    note { "$position failed" }
                    result = SyncResult.INCOMPLETE
                }
                outcome.getOrThrow() == StepOutcome.ABORT -> {
                    note { "stopped after $position" }
                    return SyncResult.ABORTED
                }
            }
        }
        return result
    }
}
