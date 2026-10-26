package com.reteno.core.internal.action.lifecycle

import android.app.Activity
import com.reteno.core.domain.model.event.Event
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.lifecycle.LifecycleEvents
import com.reteno.core.internal.platform.HostUi

internal interface TrackPermissionDialog {
    suspend operator fun invoke(activity: Activity): Result<Unit>
}

internal class TrackPermissionDialogAction(
    private val hostUi: HostUi,
    private val lifecycleEvents: LifecycleEvents,
    private val runner: ActionRunner
) : TrackPermissionDialog {

    override suspend operator fun invoke(activity: Activity): Result<Unit> = runner("TrackPermissionDialog") {
        track(activity)
    }

    private suspend fun ActionTrace.track(activity: Activity) {
        val top = step("read package of activity on top of the task") {
            hostUi.packageOnTop(activity)
        }
        if (top != PERMISSION_DIALOG_PACKAGE) {
            note { "permission dialog is not shown" }
            return
        }
        lifecycleEvents.log(this, Event.permissionDialogDisplayed())
    }

    companion object {
        const val PERMISSION_DIALOG_PACKAGE = "com.google.android.permissioncontroller"
    }
}
