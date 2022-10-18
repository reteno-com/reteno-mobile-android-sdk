package com.reteno.core.lifecycle

import android.app.Activity

interface RetenoLifecycleCallbacks {
    fun pause(activity: Activity)
    fun resume(activity: Activity)
}