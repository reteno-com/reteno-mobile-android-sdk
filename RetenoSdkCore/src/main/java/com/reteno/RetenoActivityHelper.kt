package com.reteno

import android.annotation.TargetApi
import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.os.Bundle
import com.reteno.util.BuildUtil
import com.reteno.util.Logger
import java.util.*

class RetenoActivityHelper(private val activity: Activity) {

    init {
        Reteno.applicationContext = activity.applicationContext
    }

    /**
     * Class provides additional functionality to handle payloads of push notifications built to
     * comply with new Android 12 restrictions on using notification trampolines.
     * The intent contains the message bundle which is used to run the open action and to track
     * 'Push Opened' and 'Open' events.
     */
    @TargetApi(31)
    internal class NoTrampolinesLifecycleCallbacks : RetenoLifecycleCallbacks() {
        override fun onActivityResumed(activity: Activity) {
            Logger.d("onActivityResumed(): ", "activity = [", activity, "]")
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
                Logger.e("Push Notification action not run. Did you forget reteno-push module?", t)
            }
        }
    }

    internal open class RetenoLifecycleCallbacks : ActivityLifecycleCallbacks {
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

        override fun onActivityStarted(activity: Activity) {}

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

    /**
     * Call this when your activity gets paused.
     */
    fun onPause() {
        Logger.d("onPause(): ", "activity = [", activity, "]")
        try {
            if (!registeredCallbacks) {
                onPause(activity)
            }
        } catch (t: Throwable) {
            Logger.captureException(t)
        }
    }

    /**
     * Call this when your activity gets resumed.
     */
    fun onResume() {
        Logger.d("onResume(): ", "activity = [", activity, "]")
        try {
            if (!registeredCallbacks) {
                onResume(activity)
            }
        } catch (t: Throwable) {
            Logger.captureException(t)
        }
    }

    /**
     * Call this when your activity gets stopped.
     */
    fun onStop() {
        Logger.d("onStop(): ", "activity = [", activity, "]")
        try {
            if (!registeredCallbacks) {
                onStop(activity)
            }
        } catch (t: Throwable) {
            Logger.captureException(t)
        }
    }

    companion object {
        /**
         * Retrieves if the activity is paused.
         */
        /**
         * Whether any of the activities are paused.
         */
        var isActivityPaused = false

        /**
         * Whether lifecycle callbacks were registered. This is only supported on Android OS &gt;= 4.0.
         */
        private var registeredCallbacks = false

        // keeps current activity while app is in foreground
        private var currentActivity: Activity? = null

        // keeps the last activity while app is in background, onDestroy will clear it
        private var lastForegroundActivity: Activity? = null
        private val pendingActions: Queue<Runnable> = LinkedList()

        /**
         * Set activity and run pending actions
         */
        fun setCurrentActivity(context: Context?) {
            Logger.d("setCurrentActivity(): ", "context = [", context, "]")
            if (context is Activity) {
                currentActivity = context
            }
        }

        /**
         * Retrieves the currently active activity.
         */
        fun getCurrentActivity(): Activity? {
            return currentActivity
        }

        /**
         * Enables lifecycle callbacks for Android devices with Android OS &gt;= 4.0
         */
        fun enableLifecycleCallbacks(app: Application) {
            Logger.d("enableLifecycleCallbacks(): ", "app = [", app, "]")
            Reteno.applicationContext = app.applicationContext
            if (BuildUtil.shouldDisableTrampolines(app)) {
                app.registerActivityLifecycleCallbacks(NoTrampolinesLifecycleCallbacks())
            } else {
                app.registerActivityLifecycleCallbacks(RetenoLifecycleCallbacks())
            }
            registeredCallbacks = true
        }

        private fun onPause(activity: Activity) {
            Logger.d("onPause(): ", "activity = [", activity, "]")
            isActivityPaused = true
        }

        private fun onResume(activity: Activity) {
            Logger.d("onResume(): ", "activity = [" , activity , "]")
            isActivityPaused = false
            currentActivity = activity
        }

        private fun onStop(activity: Activity) {
            Logger.d("onStop(): ", "activity = [" , activity , "]")
            // onStop is called when the activity gets hidden, and is called after onPause.
            //
            // However, if we're switching to another activity, that activity will call onResume,
            // so we shouldn't pause if that's the case.
            //
            // Thus, we can call pause from here, only if all activities are paused.
            if (isActivityPaused) {
                Reteno.pause()
            }
            if (currentActivity != null && currentActivity == activity) {
                lastForegroundActivity = currentActivity
                // Don't leak activities.
                currentActivity = null
            }
        }

        private fun onDestroy(activity: Activity) {
            Logger.d("onDestroy(): ", "activity = [" , activity , "]")
            if (isActivityPaused && lastForegroundActivity != null && lastForegroundActivity == activity) {
                // prevent activity leak
                lastForegroundActivity = null
                // no activity is presented and last activity is being destroyed
                // dismiss inapp dialogs to prevent leak
                // TODO: Not yet implemented
            }
        }

        /**
         * Enqueues a callback to invoke when an activity reaches in the foreground.
         */
        internal fun queueActionUponActive(action: Runnable) {
            try {
                if (canPresentMessages()) {
                    action.run()
                } else {
                    synchronized(pendingActions) {
                        pendingActions.add(action)
                    }
                }
            } catch (t: Throwable) {
                Logger.captureException(t)
            }
        }

        /**
         * Checks whether activity is in foreground.
         */
        internal fun canPresentMessages(): Boolean {
            return (currentActivity != null && !currentActivity!!.isFinishing
                    && !isActivityPaused)
        }

        /**
         * Runs any pending actions that have been queued.
         */
        private fun runPendingActions() {
            if (isActivityPaused || currentActivity == null) {
                // Trying to run pending actions, but no activity is resumed. Skip.
                return
            }
            var runningActions: Queue<Runnable>
            synchronized(pendingActions) {
                runningActions = LinkedList(pendingActions)
                pendingActions.clear()
            }
            for (action in runningActions) {
                action.run()
            }
        }
    }
}