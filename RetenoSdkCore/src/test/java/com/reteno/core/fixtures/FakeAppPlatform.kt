package com.reteno.core.fixtures

import android.app.Activity
import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.domain.model.device.Device
import com.reteno.core.internal.platform.AppPlatform
import com.reteno.core.internal.platform.AppVersion
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

    override fun watchAppLifecycle(onForeground: () -> Unit, onBackground: () -> Unit, onActivityPaused: (Activity) -> Unit) {
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
}
