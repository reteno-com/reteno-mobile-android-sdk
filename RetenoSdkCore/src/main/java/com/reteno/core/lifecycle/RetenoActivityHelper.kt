package com.reteno.core.lifecycle

import android.annotation.TargetApi
import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import com.reteno.core.RetenoImpl
import com.reteno.core.domain.controller.EventController
import com.reteno.core.util.BuildUtil
import com.reteno.core.util.Logger
import com.reteno.core.util.getAppName
import java.util.*
import kotlin.collections.HashMap


class RetenoActivityHelper(private val eventController: EventController) {

    private val retenoLifecycleCallbacks: MutableMap<String, RetenoLifecycleCallbacks> =
        Collections.synchronizedMap(HashMap())

    /**
     * Retrieves if the activity is paused.
     */
    /**
     * Whether any of the activities are paused.
     */
    private var isActivityPaused = false

    // keeps current activity while app is in foreground
    private var currentActivity: Activity? = null

    // keeps the last activity while app is in background, onDestroy will clear it
    private var lastForegroundActivity: Activity? = null

    private var screenTrackingConfig = ScreenTrackingConfig(true)

    private val fragmentLifecycleCallbacks = object : FragmentLifecycleCallbacks() {
        override fun onFragmentStarted(fragmentManager: FragmentManager, fragment: Fragment) {
            super.onFragmentStarted(fragmentManager, fragment)
            val fragmentName = fragment::class.java.simpleName

            if (screenTrackingConfig.trigger == ScreenTrackingTrigger.ON_START
                && screenTrackingConfig.enable
                && !screenTrackingConfig.excludeScreens.contains(fragmentName)
            ) {
                /*@formatter:off*/ Logger.i(TAG, "onFragmentStarted(): ", "trackScreen $fragmentName")
                /*@formatter:on*/
                eventController.trackScreenViewEvent(fragmentName)
            }
        }

        override fun onFragmentResumed(fragmentManager: FragmentManager, fragment: Fragment) {
            super.onFragmentResumed(fragmentManager, fragment)
            val fragmentName = fragment::class.java.simpleName

            if (screenTrackingConfig.trigger == ScreenTrackingTrigger.ON_RESUME
                && screenTrackingConfig.enable
                && !screenTrackingConfig.excludeScreens.contains(fragmentName)
            ) {
                /*@formatter:off*/ Logger.i(TAG, "onFragmentResumed(): ", "trackScreen $fragmentName")
                /*@formatter:on*/
                eventController.trackScreenViewEvent(fragmentName)
            }
        }
    }

    /**
     * Enables lifecycle callbacks for Android devices with Android OS &gt;= 4.0
     */
    fun enableLifecycleCallbacks(callbacks: RetenoLifecycleCallbacks) {
        val app = RetenoImpl.application
        /*@formatter:off*/ Logger.i(TAG, "enableLifecycleCallbacks(): ", "callbacks = [" , callbacks , "], app = [" , app , "]")
        /*@formatter:on*/
        registerActivityLifecycleCallbacks(app.getAppName(), callbacks)
        if (BuildUtil.shouldDisableTrampolines()) {
            app.registerActivityLifecycleCallbacks(NoTrampolinesLifecycleCallbacks())
        } else {
            app.registerActivityLifecycleCallbacks(RetenoActivityLifecycleCallbacks())
        }
    }

    fun autoScreenTracking(config: ScreenTrackingConfig) {
        /*@formatter:off*/ Logger.i(TAG, "autoScreenTracking(): ", "config = [" , config , "]")
        /*@formatter:on*/
        screenTrackingConfig = config
    }

    fun registerActivityLifecycleCallbacks(
        key: String,
        callbacks: RetenoLifecycleCallbacks
    ) {
        /*@formatter:off*/ Logger.i(TAG, "registerActivityLifecycleCallbacks(): ", "key = [" , key , "], callbacks = [" , callbacks , "]")
        /*@formatter:on*/
        synchronized(retenoLifecycleCallbacks) {
            retenoLifecycleCallbacks[key] = callbacks
        }
        currentActivity?.let(callbacks::resume)
    }

    fun unregisterActivityLifecycleCallbacks(key: String) {
        /*@formatter:off*/ Logger.i(TAG, "unregisterActivityLifecycleCallbacks(): ", "key = [" , key , "]")
        /*@formatter:on*/
        synchronized(retenoLifecycleCallbacks) {
            retenoLifecycleCallbacks.remove(key);
        }
    }


    private fun onStart(activity: Activity) {
        /*@formatter:off*/ Logger.i(TAG, "onStart(): ", "activity = [" , activity , "]")
        /*@formatter:on*/
        (activity as? FragmentActivity)?.supportFragmentManager?.registerFragmentLifecycleCallbacks(
            fragmentLifecycleCallbacks,
            true
        )
    }

    private fun onResume(activity: Activity) {
        /*@formatter:off*/ Logger.i(TAG, "onResume(): ", "activity = [" , activity , "]")
        /*@formatter:on*/
        isActivityPaused = false
        currentActivity = activity
        notifyLifecycleCallbacksResumed(activity)
    }

    private fun notifyLifecycleCallbacksResumed(activity: Activity) {
        /*@formatter:off*/ Logger.i(TAG, "notifyLifecycleCallbacksResumed(): ", "callbacks.size = [" , retenoLifecycleCallbacks.size , "]")
        /*@formatter:on*/
        synchronized(retenoLifecycleCallbacks) {
            for ((_, value) in retenoLifecycleCallbacks.entries) {
                value.resume(activity)
            }
        }
    }


    private fun onPause(activity: Activity) {
        /*@formatter:off*/ Logger.i(TAG, "onPause(): ", "activity = [" , activity , "]")
        /*@formatter:on*/
        isActivityPaused = true
    }

    private fun onStop(activity: Activity) {
        /*@formatter:off*/ Logger.i(TAG, "onStop(): ", "activity = [" , activity , "]")
        /*@formatter:on*/
        // onStop is called when the activity gets hidden, and is called after onPause.
        //
        // However, if we're switching to another activity, that activity will call onResume,
        // so we shouldn't pause if that's the case.
        //
        // Thus, we can call pause from here, only if all activities are paused.
        if (isActivityPaused) {
            notifyLifecycleCallbacksPaused(activity)
        }
        if (currentActivity != null && currentActivity == activity) {
            lastForegroundActivity = currentActivity
            // Don't leak activities.
            currentActivity = null
        }

        (activity as? FragmentActivity)?.supportFragmentManager?.unregisterFragmentLifecycleCallbacks(
            fragmentLifecycleCallbacks
        )
    }

    private fun notifyLifecycleCallbacksPaused(activity: Activity) {
        /*@formatter:off*/ Logger.i(TAG, "notifyLifecycleCallbacksPaused(): ", "callbacks.size = [" , retenoLifecycleCallbacks.size , "]")
        /*@formatter:on*/
        synchronized(retenoLifecycleCallbacks) {
            for ((_, value) in retenoLifecycleCallbacks.entries) {
                value.pause(activity)
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
            // dismiss inapp dialogs to prevent leak
            // TODO: Not yet implemented
        }
    }

    /**
     * Checks whether activity is in foreground.
     */
    internal fun canPresentMessages(): Boolean =
        currentActivity != null && !currentActivity!!.isFinishing && !isActivityPaused

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
            // TODO: Not implemented yet
//            if (activity.intent != null) {
//                val extras = activity.intent.extras
//                if (extras != null && extras.containsKey(Constants.Keys.PUSH_MESSAGE_TEXT)) {
//                    OperationQueue.sharedInstance().addParallelOperation { handleNotificationPayload(extras) }
//                }
//            }
        }

        // TODO: Not used yet
        private fun handleNotificationPayload(message: Bundle) {
            try {
                Class.forName("com.reteno.RetenoPushService")
                    .getDeclaredMethod("onActivityNotificationClick", Bundle::class.java)
                    .invoke(null, message)
            } catch (t: Throwable) {
                Logger.captureException(t)
                Logger.e(
                    TAG,
                    "Push Notification action not run. Did you forget reteno-push module?",
                    t
                )
            }
        }
    }

    open inner class RetenoActivityLifecycleCallbacks : ActivityLifecycleCallbacks {
        override fun onActivityStopped(activity: Activity) {
            try {
                onStop(activity)
            } catch (t: Throwable) {
                Logger.captureException(t)
            }
        }

        override fun onActivityResumed(activity: Activity) {
            try {
                onResume(activity)
            } catch (t: Throwable) {
                Logger.captureException(t)
            }
        }

        override fun onActivityPaused(activity: Activity) {
            try {
                onPause(activity)
            } catch (t: Throwable) {
                Logger.captureException(t)
            }
        }

        override fun onActivityStarted(activity: Activity) {
            try {
                onStart(activity)
            } catch (t: Throwable) {
                Logger.captureException(t)
            }
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {
            try {
                onDestroy(activity)
            } catch (t: Throwable) {
                Logger.captureException(t)
            }
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    }

    companion object {
        val TAG: String = RetenoActivityHelper::class.java.simpleName
    }
}