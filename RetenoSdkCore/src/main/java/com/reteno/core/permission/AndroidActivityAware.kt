package com.reteno.core.permission

import android.app.Activity
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.annotation.CallSuper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

open class AndroidActivityAware : DefaultLifecycleObserver {
    @Volatile
    private var activity: ComponentActivity? = null

    suspend fun awaitActivity(): Activity = withTimeout(ACTIVITY_WAIT_MILLIS) { attachedActivity() }

    private suspend fun attachedActivity(): Activity {
        while (true) {
            val attached = activity
            if (attached != null) return attached
            delay(ACTIVITY_POLL_MILLIS)
        }
    }

    suspend fun awaitContext(): Context = awaitActivity()

    @CallSuper
    open fun attachActivity(activity: ComponentActivity) {
        this.activity = activity
        activity.lifecycle.addObserver(this)
    }

    @CallSuper
    override fun onDestroy(owner: LifecycleOwner) {
        owner.lifecycle.removeObserver(this)
        activity = null
        super.onDestroy(owner)
    }

    private companion object {
        const val ACTIVITY_WAIT_MILLIS = 2_000L
        const val ACTIVITY_POLL_MILLIS = 20L
    }
}
