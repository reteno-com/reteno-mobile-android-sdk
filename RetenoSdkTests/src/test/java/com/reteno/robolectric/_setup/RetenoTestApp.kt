package com.reteno.robolectric._setup

import android.app.Application
import com.reteno.core.Reteno
import com.reteno.core.RetenoApplication
import com.reteno.core.RetenoImpl

class RetenoTestApp : Application(), RetenoApplication {
    private lateinit var retenoInstance: Reteno

    override fun onCreate() {
        super.onCreate()
        retenoInstance = RetenoImpl(this)
    }

    override fun getRetenoInstance(): Reteno {
        return retenoInstance
    }
}
