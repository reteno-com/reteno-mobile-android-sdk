package com.reteno.core.lifecycle

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import com.reteno.core.util.Logger
import java.util.Collections

internal class RetenoActivityHelperImpl : RetenoActivityHelper {

    private val retenoLifecycleCallbacks: MutableMap<String, RetenoLifecycleCallbacks> =
        Collections.synchronizedMap(LinkedHashMap())

    private var isActivityPaused = false

    private var isReadyForTransition = false

    override var currentActivity: Activity? = null
        private set

    internal val lifecycleCallbacks = LifecycleCallbacks()

    internal inner class LifecycleCallbacks : ActivityLifecycleCallbacks {

        override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
            isReadyForTransition = true
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

        override fun onActivityStarted(activity: Activity) {
            notifyCallbacks("onActivityStarted") { it.start(activity) }
        }

        override fun onActivityResumed(activity: Activity) {
            isActivityPaused = false
            isReadyForTransition = false
            currentActivity = activity
            notifyCallbacks("onActivityResumed") { it.resume(activity) }
        }

        override fun onActivityPaused(activity: Activity) {
            isActivityPaused = true
            notifyCallbacks("onActivityPaused") { it.pause(activity) }
        }

        override fun onActivityStopped(activity: Activity) {
            notifyCallbacks("onActivityStopped") { it.stop(activity) }
            if (currentActivity == activity) {
                currentActivity = null
            }
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

        override fun onActivityDestroyed(activity: Activity) = Unit
    }

    override fun enableLifecycleCallbacks(application: Application) {
        application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
    }

    override fun registerActivityLifecycleCallbacks(key: String, callbacks: RetenoLifecycleCallbacks) {
        synchronized(retenoLifecycleCallbacks) {
            retenoLifecycleCallbacks[key] = callbacks
            val activity = currentActivity ?: return
            callbacks.start(activity)
            callbacks.resume(activity)
        }
    }

    override fun unregisterActivityLifecycleCallbacks(key: String) {
        synchronized(retenoLifecycleCallbacks) {
            retenoLifecycleCallbacks.remove(key)
        }
    }

    override fun canPresentMessages(): Boolean {
        val activity = currentActivity ?: return false
        return !activity.isFinishing && !isActivityPaused && !isReadyForTransition
    }

    // Ensures the Activity is fully ready by;
    //   1. Ensure it is attached to a top-level Window by checking if it has an IBinder
    //   2. Ensure WindowInsets exists on the root window also
    override fun isActivityFullyReady(): Boolean {
        val activity = currentActivity ?: return false
        val decorView = activity.window.decorView
        return decorView.applicationWindowToken != null && decorView.rootWindowInsets != null
    }

    private fun notifyCallbacks(event: String, notify: (RetenoLifecycleCallbacks) -> Unit) {
        val callbacks = synchronized(retenoLifecycleCallbacks) { retenoLifecycleCallbacks.values.toList() }
        callbacks.forEach { callback ->
            try {
                notify(callback)
            } catch (e: Exception) {
                Logger.e(TAG, "$event(): ", e)
            }
        }
    }

    private companion object {
        const val TAG = "RetenoActivityHelperImpl"
    }
}
