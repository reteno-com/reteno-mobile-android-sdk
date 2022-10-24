package com.reteno.core.base.robolectric._setup

import android.app.Application
import com.reteno.core.Reteno
import com.reteno.core.RetenoApplication
import com.reteno.core.RetenoImpl
import com.reteno.core.util.Logger
import io.mockk.*

class RetenoTestApp : Application(), RetenoApplication {
    private lateinit var retenoInstance: Reteno

    override fun onCreate() {
        super.onCreate()

        mockkStatic(Logger::class)
        every { Logger.v(any(), any(), *anyVararg()) } just runs
        every { Logger.d(any(), any(), *anyVararg()) } just runs
        every { Logger.i(any(), any(), *anyVararg()) } just runs
        every { Logger.w(any(), any(), *anyVararg()) } just runs
        every { Logger.e(any(), any())} just runs
        every { Logger.e(any(), any(), any()) } just runs
        every { Logger.captureException(any()) } just runs
        every { Logger.captureEvent(any()) } just runs

        retenoInstance = RetenoImpl(this)
    }

    override fun getRetenoInstance(): Reteno {
        return retenoInstance
    }
}
