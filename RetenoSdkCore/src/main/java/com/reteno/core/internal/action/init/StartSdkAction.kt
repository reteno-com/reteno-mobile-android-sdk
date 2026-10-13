package com.reteno.core.internal.action.init

import com.reteno.core.domain.controller.IamController
import com.reteno.core.internal.action.device.EnsureDeviceRegisteredThisSession
import com.reteno.core.internal.action.sync.SyncScheduler
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.lifecycle.SdkRunState
import com.reteno.core.lifecycle.RetenoSessionHandler
import com.reteno.core.view.iam.IamView
import kotlinx.coroutines.withContext

internal interface StartSdk {
    suspend operator fun invoke(): Result<Unit>
}

internal class StartSdkAction(
    private val ensureDeviceRegistered: EnsureDeviceRegisteredThisSession,
    private val sessionHandler: RetenoSessionHandler,
    private val iamController: IamController,
    private val dataSync: SyncScheduler,
    private val iamView: IamView,
    private val state: SdkRunState,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) : StartSdk {

    override suspend operator fun invoke(): Result<Unit> = runner("StartSdk") { start() }

    private suspend fun ActionTrace.start() {
        withContext(dispatchers.io) { ensureDeviceRegistered() }
        step("start session tracking") { sessionHandler.start() }
        if (state.markStarted()) {
            step("fetch in-app messages from server") { withContext(dispatchers.io) { iamController.getInAppMessages() } }
        } else {
            note { "sdk already started, in-app messages are fetched" }
        }
        step("start data sync scheduler") { dataSync.startScheduler() }
        step("start in-app message view") { withContext(dispatchers.main) { iamView.start() } }
    }
}
