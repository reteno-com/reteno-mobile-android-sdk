package com.reteno.core.internal.action.sync

import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.action.trace.TracedOutcome

internal enum class StepOutcome(override val isSuccessful: Boolean) : TracedOutcome {
    CONTINUE(true),
    ABORT(false)
}

internal fun ActionTrace.nothingToSend(): StepOutcome {
    note { "nothing to send" }
    return StepOutcome.CONTINUE
}

internal fun ActionTrace.finishedAfter(handled: Int): StepOutcome =
    if (handled == 0) nothingToSend() else StepOutcome.CONTINUE

internal fun interface SyncStep {
    suspend operator fun invoke(): Result<StepOutcome>
}
