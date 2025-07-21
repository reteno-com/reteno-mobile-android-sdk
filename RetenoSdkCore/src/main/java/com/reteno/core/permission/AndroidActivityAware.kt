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

    protected fun requireActivity(): Activity = requireNotNull(activity)

    suspend fun awaitActivity() = withTimeout(2000L) {
        if (activity != null) return@withTimeout requireActivity()
        while (activity == null) {
            delay(20)
        }
        return@withTimeout requireActivity()
    }

    suspend fun awaitContext(): Context = awaitActivity()

    @CallSuper
    open fun attachActivity(activity: ComponentActivity) {
        this.activity = activity
        activity.lifecycle.addObserver(this)
    }

    @CallSuper
    override fun onDestroy(owner: LifecycleOwner) {
        activity?.lifecycle?.removeObserver(this)
        activity = null
        super.onDestroy(owner)
    }
}