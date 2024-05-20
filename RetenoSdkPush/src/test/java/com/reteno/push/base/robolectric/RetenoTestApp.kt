package com.reteno.push.base.robolectric

import android.app.Application
import android.provider.Settings
import com.reteno.core.Reteno
import com.reteno.core.RetenoApplication
import com.reteno.core.RetenoImpl
import com.reteno.core.util.BuildUtil
import com.reteno.core.util.Logger
import com.reteno.push.Util
import com.reteno.push.channel.RetenoNotificationChannel
import com.reteno.push.interceptor.click.IntentHandler
import com.reteno.push.receiver.NotificationsEnabledManager
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic

class RetenoTestApp : Application(), RetenoApplication {

    internal var retenoMock: RetenoImpl = mockk()

    init {
        mockLogger()
        mockkObject(NotificationsEnabledManager)
        mockkObject(RetenoNotificationChannel)
        mockkObject(Util)
        mockkObject(IntentHandler.AppLaunchIntent)
        mockkObject(BuildUtil)
        mockkStatic(Util::class)
    }

    override fun onCreate() {
        super.onCreate()
        Settings.Secure.putString(contentResolver, Settings.Secure.ANDROID_ID, Constants.DEVICE_ID_ANDROID)
    }

    override fun getRetenoInstance(): Reteno {
        return retenoMock
    }

    private fun mockLogger() {
        mockkStatic(Logger::class)
        justRun { Logger.v(any(), any(), *anyVararg()) }
        justRun { Logger.d(any(), any(), *anyVararg()) }
        justRun { Logger.i(any(), any(), *anyVararg()) }
        justRun { Logger.w(any(), any(), *anyVararg()) }
        justRun { Logger.e(any(), any(), any()) }
        justRun { Logger.captureMessage(any()) }
    }
}
