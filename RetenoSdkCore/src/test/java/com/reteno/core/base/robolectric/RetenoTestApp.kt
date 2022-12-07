package com.reteno.core.base.robolectric

import android.app.Application
import android.provider.Settings
import com.reteno.core.Reteno
import com.reteno.core.RetenoApplication
import com.reteno.core.RetenoImpl
import com.reteno.core.di.ServiceLocator
import com.reteno.core.util.Logger
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockkStatic
import io.mockk.spyk

class RetenoTestApp : Application(), RetenoApplication {
    private lateinit var retenoInstance: Reteno

    override fun onCreate() {
        super.onCreate()

        Settings.Secure.putString(contentResolver, Settings.Secure.ANDROID_ID, Constants.DEVICE_ID_ANDROID)
        mockStaticLogger()

        retenoInstance = spyk(RetenoImpl(this, ""))
        every { retenoInstance.getProperty("serviceLocator") } returns spyk(ServiceLocator(this, ""))
    }

    override fun getRetenoInstance(): Reteno {
        return retenoInstance
    }

    private fun mockStaticLogger() {
        mockkStatic(Logger::class)
        justRun { Logger.v(any(), any(), *anyVararg()) }
        justRun { Logger.d(any(), any(), *anyVararg()) }
        justRun { Logger.i(any(), any(), *anyVararg()) }
        justRun { Logger.w(any(), any(), *anyVararg()) }
        justRun { Logger.e(any(), any()) }
        justRun { Logger.e(any(), any(), any()) }
        justRun { Logger.captureException(any()) }
        justRun { Logger.captureEvent(any()) }
    }
}
