package com.reteno.core.fixtures

import com.reteno.core.internal.action.sync.StepOutcome
import com.reteno.core.internal.action.sync.SyncStep

internal fun syncStep(stepName: String, outcome: suspend () -> Result<StepOutcome>): SyncStep = object : SyncStep {
    override val name: String = stepName

    override suspend fun invoke(): Result<StepOutcome> = outcome()
}
