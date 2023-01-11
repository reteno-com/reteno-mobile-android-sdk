package com.reteno.core.domain.controller

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.lifecycle.RetenoLifecycleCallbacks
import com.reteno.core.lifecycle.ScreenTrackingConfig
import com.reteno.core.lifecycle.ScreenTrackingTrigger
import com.reteno.core.util.Logger

internal class ScreenTrackingController(private val retenoActivityHelper: RetenoActivityHelper, private val eventController: EventController) {

    private var screenTrackingConfig = ScreenTrackingConfig(true)

    private val fragmentLifecycleCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
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


    init {
        retenoActivityHelper.registerActivityLifecycleCallbacks(TAG, object : RetenoLifecycleCallbacks{
            override fun pause(activity: Activity) {
            }

            override fun resume(activity: Activity) {
            }

            override fun start(activity: Activity) {
                (activity as? FragmentActivity)?.supportFragmentManager?.registerFragmentLifecycleCallbacks(
                    fragmentLifecycleCallbacks,
                    true
                )
            }

            override fun stop(activity: Activity) {
                (activity as? FragmentActivity)?.supportFragmentManager?.unregisterFragmentLifecycleCallbacks(
                    fragmentLifecycleCallbacks
                )
            }

        })
    }


    internal fun autoScreenTracking(config: ScreenTrackingConfig) {
        /*@formatter:off*/ Logger.i(TAG, "autoScreenTracking(): ", "config = [" , config , "]")
        /*@formatter:on*/
        screenTrackingConfig = config
    }

    companion object {
        private val TAG: String = ScreenTrackingController::class.java.simpleName
    }
}