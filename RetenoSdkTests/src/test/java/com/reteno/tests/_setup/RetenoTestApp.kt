package com.reteno.tests._setup

import android.app.Application
import com.reteno.Reteno
import com.reteno.RetenoApplication
import com.reteno.RetenoImpl

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
