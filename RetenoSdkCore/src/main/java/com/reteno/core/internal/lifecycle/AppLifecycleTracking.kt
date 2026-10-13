package com.reteno.core.internal.lifecycle

import com.reteno.core.internal.action.lifecycle.AppBackgrounded
import com.reteno.core.internal.action.lifecycle.AppForegrounded
import com.reteno.core.internal.action.lifecycle.TrackPermissionDialog
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.coroutine.SerialQueue
import com.reteno.core.internal.platform.AppPlatform
import kotlinx.coroutines.withContext

internal interface AppLifecycleTracking {
    fun start()
}

internal class AppLifecycleTrackingImpl(
    private val platform: AppPlatform,
    private val appForegrounded: AppForegrounded,
    private val appBackgrounded: AppBackgrounded,
    private val trackPermissionDialog: TrackPermissionDialog,
    private val queue: SerialQueue,
    private val dispatchers: RetenoDispatchers
) : AppLifecycleTracking {

    override fun start() {
        platform.watchAppLifecycle(
            onForeground = { queue.enqueue { withContext(dispatchers.io) { appForegrounded() } } },
            onBackground = { queue.enqueue { withContext(dispatchers.io) { appBackgrounded() } } },
            onActivityPaused = { activity -> queue.enqueue { trackPermissionDialog(activity) } }
        )
    }
}
