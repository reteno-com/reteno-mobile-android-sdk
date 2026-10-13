package com.reteno.core.internal.action.push

import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.view.iam.IamView
import kotlinx.coroutines.withContext

internal class InitializeIamFromPushAction(
    private val iamView: IamView,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) {

    suspend operator fun invoke(interactionId: String): Result<Unit> = runner("InitializeIamFromPush") {
        args("interactionId" to interactionId)
        step("show in-app message from push") {
            withContext(dispatchers.main) { iamView.initialize(interactionId) }
        }
    }
}
