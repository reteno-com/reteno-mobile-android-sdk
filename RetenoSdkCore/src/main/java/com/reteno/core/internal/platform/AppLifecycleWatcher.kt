package com.reteno.core.internal.platform

import android.app.Activity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.lifecycle.RetenoLifecycleCallBacksAdapter

internal class AppLifecycleWatcher(
    private val lifecycle: Lifecycle,
    private val activityHelper: RetenoActivityHelper,
    private val onForeground: () -> Unit,
    private val onBackground: () -> Unit,
    private val onActivityPaused: (Activity) -> Unit
) {

    private val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_START) onForeground()
        if (event == Lifecycle.Event.ON_STOP) onBackground()
    }

    fun register() {
        val pauseCallbacks = RetenoLifecycleCallBacksAdapter(onPause = onActivityPaused)
        activityHelper.registerActivityLifecycleCallbacks(KEY, pauseCallbacks)
        lifecycle.addObserver(observer)
    }

    private companion object {
        const val KEY = "AppLifecycleWatcher"
    }
}
