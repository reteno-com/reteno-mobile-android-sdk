package com.reteno.core.lifecycle

import android.annotation.TargetApi
import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import com.reteno.core.util.BuildUtil
import com.reteno.core.util.Logger
import java.util.Collections


internal class RetenoActivityHelperImpl : RetenoActivityHelper {

    private val retenoLifecycleCallbacks: MutableMap<String, RetenoLifecycleCallbacks> =
        Collections.synchronizedMap(HashMap())

    /**
     * Retrieves if the activity is paused.
     */
    /**
     * Whether any of the activities are paused.
     */
    private var isActivityPaused = false
    /**
     * Retrieves if the activity is ready to be changed.
     */
    private var isReadyForTransition = false

    // keeps current activity while app is in foreground
    override var currentActivity: Activity? = null
        private set

    // keeps the last activity while app is in background, onDestroy will clear it
    private var lastForegroundActivity: Activity? = null

    override fun enableLifecycleCallbacks(application: Application) {
        /*@formatter:off*/ Logger.i(TAG, "enableLifecycleCallbacks(): ", "app = [" , application , "]")
        /*@formatter:on*/
        if (BuildUtil.shouldDisableTrampolines(application)) {
            application.registerActivityLifecycleCallbacks(NoTrampolinesLifecycleCallbacks())
        } else {
            application.registerActivityLifecycleCallbacks(RetenoActivityLifecycleCallbacks())
        }
    }

    override fun registerActivityLifecycleCallbacks(
        key: String,
        callbacks: RetenoLifecycleCallbacks
    ) {
        synchronized(retenoLifecycleCallbacks) {
            /*@formatter:off*/ Logger.i(TAG, "registerActivityLifecycleCallbacks(): ", "key = [" , key , "], callbacks = [" , callbacks , "]")
            /*@formatter:on*/
            retenoLifecycleCallbacks[key] = callbacks
            currentActivity?.let(callbacks::start)
            currentActivity?.let(callbacks::resume)
        }
    }

    override fun unregisterActivityLifecycleCallbacks(key: String) {
        synchronized(retenoLifecycleCallbacks) {
            /*@formatter:off*/ Logger.i(TAG, "unregisterActivityLifecycleCallbacks(): ", "key = [" , key , "]")
            /*@formatter:on*/
            retenoLifecycleCallbacks.remove(key);
        }
    }

    private fun onActivityPreCreated(activity: Activity) {
        /*@formatter:off*/ Logger.i(TAG, "onActivityPreCreated(): ", "activity = [" , activity , "]")
        /*@formatter:on*/
        isReadyForTransition = true
    }


    private fun onStart(activity: Activity) {
        /*@formatter:off*/ Logger.i(TAG, "onStart(): ", "activity = [" , activity , "]")
        /*@formatter:on*/
        notifyLifecycleCallbacksStarted(activity)
    }

    private fun onResume(activity: Activity) {
        /*@formatter:off*/ Logger.i(TAG, "onResume(): ", "activity = [" , activity , "]")
        /*@formatter:on*/
        isActivityPaused = false
        isReadyForTransition = false
        currentActivity = activity
        notifyLifecycleCallbacksResumed(activity)
    }


    private fun onPause(activity: Activity) {
        /*@formatter:off*/ Logger.i(TAG, "onPause(): ", "activity = [" , activity , "]")
        /*@formatter:on*/
        isActivityPaused = true
        notifyLifecycleCallbacksPaused(activity)
    }

    private fun onStop(activity: Activity) {
        /*@formatter:off*/ Logger.i(TAG, "onStop(): ", "activity = [" , activity , "]")
        /*@formatter:on*/
        notifyLifecycleCallbacksStopped(activity)

        if (currentActivity != null && currentActivity == activity) {
            lastForegroundActivity = currentActivity
            // Don't leak activities.
            currentActivity = null
        }
    }

    private fun notifyLifecycleCallbacksStarted(activity: Activity) {
        synchronized(retenoLifecycleCallbacks) {
            /*@formatter:off*/ Logger.i(TAG, "notifyLifecycleCallbacksStarted(): ", "callbacks.size = [" , retenoLifecycleCallbacks.size , "]")
            /*@formatter:on*/
            for ((_, value) in retenoLifecycleCallbacks.entries) {
                value.start(activity)
            }
        }
    }

    private fun notifyLifecycleCallbacksResumed(activity: Activity) {
        synchronized(retenoLifecycleCallbacks) {
            /*@formatter:off*/ Logger.i(TAG, "notifyLifecycleCallbacksResumed(): ", "callbacks.size = [" , retenoLifecycleCallbacks.size , "]")
            /*@formatter:on*/
            for ((_, value) in retenoLifecycleCallbacks.entries) {
                value.resume(activity)
            }
        }
    }

    private fun notifyLifecycleCallbacksPaused(activity: Activity) {
        synchronized(retenoLifecycleCallbacks) {
            /*@formatter:off*/ Logger.i(TAG, "notifyLifecycleCallbacksPaused(): ", "callbacks.size = [" , retenoLifecycleCallbacks.size , "]")
            /*@formatter:on*/
            for ((_, value) in retenoLifecycleCallbacks.entries) {
                value.pause(activity)
            }
        }
    }

    private fun notifyLifecycleCallbacksStopped(activity: Activity) {
        synchronized(retenoLifecycleCallbacks) {
            /*@formatter:off*/ Logger.i(TAG, "notifyLifecycleCallbacksStopped(): ", "callbacks.size = [" , retenoLifecycleCallbacks.size , "]")
            /*@formatter:on*/
            for ((_, value) in retenoLifecycleCallbacks.entries) {
                value.stop(activity)
            }
        }
    }

    private fun onDestroy(activity: Activity) {
        /*@formatter:off*/ Logger.i(TAG, "onDestroy(): ", "activity = [" , activity , "]")
        /*@formatter:on*/
        if (isActivityPaused && lastForegroundActivity != null && lastForegroundActivity == activity) {
            // prevent activity leak
            lastForegroundActivity = null
            // no activity is presented and last activity is being destroyed
            // dismiss iam dialogs to prevent leak
            // TODO: Not yet implemented
        }
    }

    /**
     * Checks whether activity is in foreground.
     */
    override fun canPresentMessages(): Boolean =
        currentActivity != null && !currentActivity!!.isFinishing && !isActivityPaused && !isReadyForTransition

    // Ensures the Activity is fully ready by;
    //   1. Ensure it is attached to a top-level Window by checking if it has an IBinder
    //   2. Ensure WindowInsets exists on the root window also
    override fun isActivityFullyReady(): Boolean {
        val decorView = currentActivity?.window?.decorView
        val hasToken = decorView?.applicationWindowToken != null
        val insetsAttached = decorView?.rootWindowInsets != null
        val result = hasToken && insetsAttached
        /*@formatter:off*/ Logger.i(TAG, "isActivityFullyReady(): ", result)
        /*@formatter:on*/
        return result
    }

    /**
     * Class provides additional functionality to handle payloads of push notifications built to
     * comply with new Android 12 restrictions on using notification trampolines.
     * The intent contains the message bundle which is used to run the open action and to track
     * 'Push Opened' and 'Open' events.
     */
    @TargetApi(31)
    inner class NoTrampolinesLifecycleCallbacks : RetenoActivityLifecycleCallbacks() {
        override fun onActivityResumed(activity: Activity) {
            /*@formatter:off*/ Logger.i(TAG, "onActivityResumed(): ", "activity = [" , activity , "]")
            /*@formatter:on*/
            super.onActivityResumed(activity)
        }
    }

    open inner class RetenoActivityLifecycleCallbacks : ActivityLifecycleCallbacks {

        override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
            try {
                onActivityPreCreated(activity)
            } catch (t: Throwable) {
                /*@formatter:off*/ Logger.e(TAG, "onActivityPreCreated(): ", t)
                /*@formatter:on*/
            }
        }
        override fun onActivityStopped(activity: Activity) {
            try {
                onStop(activity)
            } catch (t: Throwable) {
                /*@formatter:off*/ Logger.e(TAG, "onActivityStopped(): ", t)
                /*@formatter:on*/
            }
        }

        override fun onActivityResumed(activity: Activity) {
            try {
                onResume(activity)
            } catch (t: Throwable) {
                /*@formatter:off*/ Logger.e(TAG, "onActivityResumed(): ", t)
                /*@formatter:on*/
            }
        }

        override fun onActivityPaused(activity: Activity) {
            try {
                onPause(activity)
            } catch (t: Throwable) {
                /*@formatter:off*/ Logger.e(TAG, "onActivityPaused(): ", t)
                /*@formatter:on*/
            }
        }

        override fun onActivityStarted(activity: Activity) {
            try {
                onStart(activity)
            } catch (t: Throwable) {
                /*@formatter:off*/ Logger.e(TAG, "onActivityStarted(): ", t)
                /*@formatter:on*/
            }
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {
            try {
                onDestroy(activity)
            } catch (t: Throwable) {
                /*@formatter:off*/ Logger.e(TAG, "onActivityDestroyed(): ", t)
                /*@formatter:on*/
            }
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    }

    companion object {
        private val TAG: String = RetenoActivityHelperImpl::class.java.simpleName
    }
}