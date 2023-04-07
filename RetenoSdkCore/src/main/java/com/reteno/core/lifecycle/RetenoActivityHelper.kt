package com.reteno.core.lifecycle

import android.app.Activity


interface RetenoActivityHelper {

    val currentActivity: Activity?

    /**
     * Enables lifecycle callbacks for Android devices with Android OS &gt;= 4.0
     */
    fun enableLifecycleCallbacks(callbacks: RetenoLifecycleCallbacks)

    fun registerActivityLifecycleCallbacks(
        key: String,
        callbacks: RetenoLifecycleCallbacks
    )

    fun unregisterActivityLifecycleCallbacks(key: String)

    fun canPresentMessages(): Boolean

    fun isActivityFullyReady(): Boolean
}