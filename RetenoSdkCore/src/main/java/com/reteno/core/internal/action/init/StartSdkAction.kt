package com.reteno.core.internal.action.init

import com.reteno.core.domain.controller.IamController
import com.reteno.core.internal.action.device.EnsureDeviceRegisteredThisSession
import com.reteno.core.internal.action.iam.FetchInAppMessages
import com.reteno.core.internal.action.inbox.InboxCountPolling
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
    private val fetchInAppMessages: FetchInAppMessages,
    private val dataSync: SyncScheduler,
    private val iamView: IamView,
    private val inboxCount: InboxCountPolling,
    private val state: SdkRunState,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) : StartSdk {

    override suspend operator fun invoke(): Result<Unit> = runner("StartSdk") {
        start()
    }

    private suspend fun ActionTrace.start() {
        withContext(dispatchers.io) { ensureDeviceRegistered() }
        step("start session tracking") {
            sessionHandler.start()
        }
        if (state.markStarted()) {
            val messages = withContext(dispatchers.io) { fetchInAppMessages() }.getOrDefault(emptyList())
            step("schedule in-app messages by their triggers") {
                withContext(dispatchers.io) { iamController.processInAppMessages(messages) }
            }
        } else {
            note { "sdk already started, in-app messages are fetched" }
        }
        step("start data sync scheduler") {
            dataSync.startScheduler()
        }
        step("start in-app message view") {
            withContext(dispatchers.main) { iamView.start() }
        }
        step("resume inbox count polling") {
            inboxCount.resume()
        }
    }
}
