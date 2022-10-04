package com.reteno

import android.app.Activity
import android.app.Application

/**
 * Base class for your Application that handles lifecycle events.
 */
open class RetenoApplication : Application() {


    companion object {
        @JvmStatic
        var instance: RetenoApplication? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        RetenoActivityHelper.enableLifecycleCallbacks(this)
    }
}