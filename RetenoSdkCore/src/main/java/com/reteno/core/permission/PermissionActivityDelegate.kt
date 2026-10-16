package com.reteno.core.permission

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import com.reteno.core.util.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal class PermissionActivityDelegate(
    application: Application,
    private val mainDispatcher: CoroutineDispatcher
) : ActivityLifecycleCallbacks {

    private var currentActivity: ComponentActivity? = null
    private val activityCheckerMap = mutableMapOf<ComponentActivity, AndroidPermissionChecker>()

    init {
        application.registerActivityLifecycleCallbacks(this)
    }

    suspend fun requestChecker(): AndroidPermissionChecker? = withContext(mainDispatcher) {
        withTimeoutOrNull(CHECKER_WAIT_MILLIS) {
            resumedChecker()
        }
    }

    private suspend fun resumedChecker(): AndroidPermissionChecker {
        while (true) {
            val activity = currentActivity
            val checker = activity?.takeIf { it.isResumed() }?.let { activityCheckerMap[it] }
            if (checker != null) return checker
            delay(CHECKER_POLL_MILLIS)
        }
    }

    private fun ComponentActivity.isResumed(): Boolean =
        !isFinishing && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity !is ComponentActivity) return
        try {
            activityCheckerMap[activity] = AndroidPermissionChecker().apply { attachActivity(activity) }
        } catch (e: IllegalStateException) {
            Logger.e(TAG, "onActivityCreated(): permission requests unavailable for this activity", e)
        }
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity as? ComponentActivity
    }

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) {
        activityCheckerMap.remove(activity)
    }

    private companion object {
        const val CHECKER_WAIT_MILLIS = 2_000L
        const val CHECKER_POLL_MILLIS = 50L
        const val TAG = "PermissionActivityDelegate"
    }
}
