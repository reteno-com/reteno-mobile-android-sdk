package com.reteno.core.lifecycle

import android.app.Activity

class RetenoLifecycleCallBacksAdapter(
    private val onPause: ((Activity) -> Unit)? = null,
    private val onResume: ((Activity) -> Unit)? = null,
    private val onStart: ((Activity) -> Unit)? = null,
    private val onStop: ((Activity) -> Unit)? = null
) : RetenoLifecycleCallbacks {
    override fun pause(activity: Activity) {
        onPause?.invoke(activity)
    }

    override fun resume(activity: Activity) {
        onResume?.invoke(activity)
    }

    override fun start(activity: Activity) {
        onStart?.invoke(activity)
    }

    override fun stop(activity: Activity) {
        onStop?.invoke(activity)
    }
}