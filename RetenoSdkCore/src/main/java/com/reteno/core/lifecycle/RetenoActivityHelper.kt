package com.reteno.core.lifecycle


interface RetenoActivityHelper {

    /**
     * Enables lifecycle callbacks for Android devices with Android OS &gt;= 4.0
     */
    fun enableLifecycleCallbacks(callbacks: RetenoLifecycleCallbacks)

    fun autoScreenTracking(config: ScreenTrackingConfig)

    fun registerActivityLifecycleCallbacks(
        key: String,
        callbacks: RetenoLifecycleCallbacks
    )

    fun unregisterActivityLifecycleCallbacks(key: String)

    fun canPresentMessages(): Boolean
}