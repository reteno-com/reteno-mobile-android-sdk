package com.reteno.lifecycle

import android.app.Activity

interface RetenoLifecycleCallbacks {
    fun pause(activity: Activity)
    fun resume(activity: Activity)
}