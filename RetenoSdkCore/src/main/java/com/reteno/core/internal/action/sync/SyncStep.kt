package com.reteno.core.internal.action.sync

import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.action.trace.TracedOutcome

internal enum class StepOutcome(override val isSuccessful: Boolean) : TracedOutcome {
    CONTINUE(true),
    NOTHING_TO_SEND(true),
    ABORT(false)
}

internal fun ActionTrace.nothingToSend(): StepOutcome {
    note { "nothing to send" }
    return StepOutcome.NOTHING_TO_SEND
}

internal fun ActionTrace.finishedAfter(handled: Int): StepOutcome =
    if (handled == 0) nothingToSend() else StepOutcome.CONTINUE

internal interface SyncStep {
    val name: String

    suspend operator fun invoke(): Result<StepOutcome>
}
