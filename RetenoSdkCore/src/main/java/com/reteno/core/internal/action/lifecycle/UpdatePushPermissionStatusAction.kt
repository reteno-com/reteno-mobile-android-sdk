package com.reteno.core.internal.action.lifecycle

import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.platform.AppBroadcasts
import com.reteno.core.util.Constants

internal class UpdatePushPermissionStatusAction(
    private val broadcasts: AppBroadcasts,
    private val runner: ActionRunner
) {

    suspend operator fun invoke(): Result<Unit> = runner("UpdatePushPermissionStatus") {
        step<Int>("notify push permission receivers", detail = { "receivers=$it" }) {
            broadcasts.send(Constants.BROADCAST_ACTION_PUSH_PERMISSION_CHANGED)
        }
    }
}
