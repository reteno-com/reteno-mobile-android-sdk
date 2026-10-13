package com.reteno.core.internal.platform

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.lifecycle.RetenoLifecycleCallbacks
import com.reteno.core.lifecycle.ScreenTrackingTrigger

internal class FragmentScreenWatcher(
    private val activityHelper: RetenoActivityHelper,
    private val onScreen: (String, ScreenTrackingTrigger) -> Unit
) {

    private val fragmentCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentStarted(fragmentManager: FragmentManager, fragment: Fragment) {
            onScreen(fragment::class.java.simpleName, ScreenTrackingTrigger.ON_START)
        }

        override fun onFragmentResumed(fragmentManager: FragmentManager, fragment: Fragment) {
            onScreen(fragment::class.java.simpleName, ScreenTrackingTrigger.ON_RESUME)
        }
    }

    private val activityCallbacks = object : RetenoLifecycleCallbacks {
        override fun start(activity: Activity) {
            if (activity is FragmentActivity) {
                activity.supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentCallbacks, true)
            }
        }

        override fun stop(activity: Activity) {
            if (activity is FragmentActivity) {
                activity.supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentCallbacks)
            }
        }

        override fun pause(activity: Activity) = Unit

        override fun resume(activity: Activity) = Unit
    }

    fun register() {
        activityHelper.registerActivityLifecycleCallbacks(KEY, activityCallbacks)
    }

    private companion object {
        const val KEY = "FragmentScreenWatcher"
    }
}
