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

    private var screenTrackingConfig = ScreenTrackingConfig(enable = false)

    private val fragmentLifecycleCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentStarted(fragmentManager: FragmentManager, fragment: Fragment) {
            super.onFragmentStarted(fragmentManager, fragment)
            try {
                val fragmentName = fragment::class.java.simpleName

                if (screenTrackingConfig.trigger == ScreenTrackingTrigger.ON_START
                    && screenTrackingConfig.enable
                    && !screenTrackingConfig.excludeScreens.contains(fragmentName)
                ) {
                    /*@formatter:off*/ Logger.i(TAG, "onFragmentStarted(): ", "trackScreen $fragmentName")
                    /*@formatter:on*/
                    eventController.trackScreenViewEvent(fragmentName)
                }
            } catch (t: Throwable) {
                /*@formatter:off*/ Logger.e(TAG, "onFragmentStarted(): ", t)
                /*@formatter:on*/
            }
        }

        override fun onFragmentResumed(fragmentManager: FragmentManager, fragment: Fragment) {
            super.onFragmentResumed(fragmentManager, fragment)
            try {
                val fragmentName = fragment::class.java.simpleName

                if (screenTrackingConfig.trigger == ScreenTrackingTrigger.ON_RESUME
                    && screenTrackingConfig.enable
                    && !screenTrackingConfig.excludeScreens.contains(fragmentName)
                ) {
                    /*@formatter:off*/ Logger.i(TAG, "onFragmentResumed(): ", "trackScreen $fragmentName")
                    /*@formatter:on*/
                    eventController.trackScreenViewEvent(fragmentName)
                }
            } catch (t: Throwable) {
                /*@formatter:off*/ Logger.e(TAG, "onFragmentResumed(): ", t)
                /*@formatter:on*/
            }
        }
    }


    init {
        try {
            retenoActivityHelper.registerActivityLifecycleCallbacks(TAG, object : RetenoLifecycleCallbacks{
                override fun pause(activity: Activity) {
                    try {

                    } catch (t: Throwable) {
                        /*@formatter:off*/ Logger.e(TAG, "pause(): ", t)
                        /*@formatter:on*/
                    }
                }

                override fun resume(activity: Activity) {
                    try {

                    } catch (t: Throwable) {
                        /*@formatter:off*/ Logger.e(TAG, "resume(): ", t)
                        /*@formatter:on*/
                    }
                }

                override fun start(activity: Activity) {
                    try {
                        (activity as? FragmentActivity)?.supportFragmentManager?.registerFragmentLifecycleCallbacks(
                            fragmentLifecycleCallbacks,
                            true
                        )
                    } catch (t: Throwable) {
                        /*@formatter:off*/ Logger.e(TAG, "start(): ", t)
                        /*@formatter:on*/
                    }
                }

                override fun stop(activity: Activity) {
                    try {
                        (activity as? FragmentActivity)?.supportFragmentManager?.unregisterFragmentLifecycleCallbacks(
                            fragmentLifecycleCallbacks
                        )
                    } catch (t: Throwable) {
                        /*@formatter:off*/ Logger.e(TAG, "stop(): ", t)
                        /*@formatter:on*/
                    }
                }
            })
        } catch (t: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "init(): ", t)
            /*@formatter:on*/
        }
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