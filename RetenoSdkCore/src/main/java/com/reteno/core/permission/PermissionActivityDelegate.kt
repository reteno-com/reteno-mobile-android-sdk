package com.reteno.core.permission

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout

internal class PermissionActivityDelegate(application: Application) : ActivityLifecycleCallbacks {

    private var currentActivity: Activity? = null
    private val activityCheckerMap = mutableMapOf<Activity, AndroidPermissionChecker>()

    init {
        application.registerActivityLifecycleCallbacks(this)
    }

    suspend fun requestChecker() = runCatching {
        withTimeout(2000L) {
            while (!isResumed(currentActivity) && isActive) {
                delay(50L)
            }
            val activity = requireNotNull(currentActivity)
            activityCheckerMap[activity]
        }
    }.getOrNull()

    private fun isResumed(activity: Activity?): Boolean {
        if (activity == null) return false
        if (activity.isFinishing) return false
        return if (activity is ComponentActivity) {
            activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        } else {
            true
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        runCatching {
            val checker = activityCheckerMap.getOrPut(activity) { AndroidPermissionChecker() }
            (activity as? ComponentActivity)?.let { checker.attachActivity(it) }
        }
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }
}