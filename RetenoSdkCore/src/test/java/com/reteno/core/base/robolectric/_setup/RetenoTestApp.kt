package com.reteno.core.base.robolectric._setup

import android.app.Application
import android.util.Log
import com.reteno.core.Reteno
import com.reteno.core.RetenoApplication
import com.reteno.core.RetenoImpl
import com.reteno.core.di.ServiceLocator
import com.reteno.core.util.Logger
import io.mockk.*
import org.mockito.Matchers

class RetenoTestApp : Application(), RetenoApplication {
    private lateinit var retenoInstance: Reteno

    override fun onCreate() {
        super.onCreate()

        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), Matchers.anyString()) } returns 0
        every { Log.w(any(), Throwable()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), Throwable()) } returns 0

        mockkStatic(Logger::class)
        every { Logger.v(any(), any(), *anyVararg()) } just runs
        every { Logger.d(any(), any(), *anyVararg()) } just runs
        every { Logger.i(any(), any(), *anyVararg()) } just runs
        every { Logger.w(any(), any(), *anyVararg()) } just runs
        every { Logger.e(any(), any())} just runs
        every { Logger.e(any(), any(), any()) } just runs
        every { Logger.captureException(any()) } just runs
        every { Logger.captureEvent(any()) } just runs

        retenoInstance = spyk(RetenoImpl(this))
        every { retenoInstance.getProperty("serviceLocator") } returns spyk<ServiceLocator>()
    }

    override fun getRetenoInstance(): Reteno {
        return retenoInstance
    }
}
