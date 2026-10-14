package com.reteno.core.fixtures

import android.app.Activity
import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.domain.model.device.Device
import com.reteno.core.internal.iam.InAppScreen
import com.reteno.core.internal.platform.AppPlatform
import com.reteno.core.internal.platform.AppVersion
import com.reteno.core.internal.platform.LogEventOrigin
import com.reteno.core.lifecycle.ScreenTrackingTrigger

internal class FakeAppPlatform : AppPlatform {
    val broadcasts = mutableListOf<String>()
    var version = AppVersion("1.0", 1L)
    var topPackage: String? = null
    var onScreen: ((String, ScreenTrackingTrigger) -> Unit)? = null
    var onForeground: (() -> Unit)? = null
    var onBackground: (() -> Unit)? = null
    var onActivityPaused: ((Activity) -> Unit)? = null
    val calls = mutableListOf<String>()
    var onScheduleBackgroundSync: () -> Unit = {}
    var bundledWidget: String? = "{}"
    var onBundledWidget: () -> Unit = {}
    val screen = FakeInAppScreen()
    var origin = LogEventOrigin(
        packageName = "com.example.app",
        osVersion = "14 (API 34)",
        appVersion = "1.0 (1)",
        deviceModel = "PIXEL 8"
    )
    var originReads = 0

    override fun sendBroadcast(action: String): Int {
        broadcasts += action
        return 1
    }

    override fun installedVersion(): AppVersion = version

    override fun packageOnTop(activity: Activity): String? = topPackage

    override fun watchFragmentScreens(onScreen: (String, ScreenTrackingTrigger) -> Unit) {
        this.onScreen = onScreen
        calls += "watch fragment screens"
    }

    override fun watchAppLifecycle(
        onForeground: () -> Unit,
        onBackground: () -> Unit,
        onActivityPaused: (Activity) -> Unit
    ) {
        this.onForeground = onForeground
        this.onBackground = onBackground
        this.onActivityPaused = onActivityPaused
    }

    override fun initWorkManager() {
        calls += "init WorkManager"
    }

    override fun scheduleBackgroundSync() {
        calls += "schedule background sync"
        onScheduleBackgroundSync()
    }

    override fun cancelBackgroundSync() {
        calls += "cancel background sync"
    }

    override fun createDevice(deviceId: DeviceId, pushToken: String, pushSubscribed: Boolean): Device =
        stubDevice(deviceId, pushToken, pushSubscribed)

    override fun bundledWidget(): String? {
        onBundledWidget()
        return bundledWidget
    }

    override fun inAppScreen(): InAppScreen = screen

    override fun logEventOrigin(): LogEventOrigin {
        originReads++
        return origin
    }
}
