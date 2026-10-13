package com.reteno.core.internal.action.init

import com.reteno.core.internal.action.sync.SyncScheduler
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.lifecycle.SdkRunState
import com.reteno.core.lifecycle.RetenoSessionHandler
import com.reteno.core.view.iam.IamView
import kotlinx.coroutines.withContext

internal interface StopSdk {
    suspend operator fun invoke(): Result<Unit>
}

internal class StopSdkAction(
    private val state: SdkRunState,
    private val sessionHandler: RetenoSessionHandler,
    private val dataSync: SyncScheduler,
    private val iamView: IamView,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) : StopSdk {

    override suspend operator fun invoke(): Result<Unit> = runner("StopSdk") {
        step("mark sdk stopped") { state.markStopped() }
        step("stop session tracking") { sessionHandler.stop() }
        step("stop data sync scheduler") { dataSync.stopScheduler() }
        step("pause in-app message view") { withContext(dispatchers.main) { iamView.pause() } }
    }
}
