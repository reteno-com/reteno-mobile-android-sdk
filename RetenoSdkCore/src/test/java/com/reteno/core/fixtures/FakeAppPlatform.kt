package com.reteno.core.fixtures

import android.app.Activity
import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.domain.model.device.Device
import com.reteno.core.internal.iam.InAppScreen
import com.reteno.core.internal.platform.AppBroadcasts
import com.reteno.core.internal.platform.AppEnvironment
import com.reteno.core.internal.platform.AppVersion
import com.reteno.core.internal.platform.BackgroundWork
import com.reteno.core.internal.platform.HostUi
import com.reteno.core.internal.platform.LogEventOrigin
import com.reteno.core.lifecycle.ScreenTrackingTrigger
import java.util.Locale

internal class FakeAppPlatform : AppEnvironment, AppBroadcasts, HostUi, BackgroundWork {
    val broadcasts = mutableListOf<String>()
    var version = AppVersion("1.0", 1L)
    var topPackage: String? = null
    var onScreen: ((String, ScreenTrackingTrigger) -> Unit)? = null
    var onForeground: (() -> Unit)? = null
    var onBackground: (() -> Unit)? = null
    var onActivityPaused: ((Activity) -> Unit)? = null
    val calls = mutableListOf<String>()
    var activityTrackingStarts = 0
    var onScheduleBackgroundSync: () -> Unit = {}
    var onWatchFragmentScreens: () -> Unit = {}
    var onSend: () -> Unit = {}
    var bundledWidget: String? = "{}"
    var notificationsEnabled = true
    var notificationsReadFailure: Throwable? = null
    val notificationChannelsChecked = mutableListOf<String>()
    var onBundledWidget: () -> Unit = {}
    val screen = FakeInAppScreen()
    var origin = LogEventOrigin(
        packageName = "com.example.app",
        osVersion = "14 (API 34)",
        appVersion = "1.0 (1)",
        deviceModel = "PIXEL 8"
    )
    var originReads = 0
    var systemLocale: Locale = Locale.forLanguageTag("uk-UA")
    var appLocale: Locale = Locale.forLanguageTag("uk-UA")
    var defaultLocale: Locale = Locale.forLanguageTag("uk-UA")
    var displayedLocale: Locale? = null
    var onDisplayedLocaleRead: () -> Unit = {}
    var activityResumed = false
    private var onActivityResumed: (() -> Unit)? = null
    val likelyRegions = mutableMapOf("uk" to "UA", "en" to "US", "de" to "DE")

    override fun send(action: String): Int {
        broadcasts += action
        onSend()
        return 1
    }

    override fun installedVersion(): AppVersion = version

    override fun packageOnTop(activity: Activity): String? = topPackage

    override fun watchFragmentScreens(onScreen: (String, ScreenTrackingTrigger) -> Unit) {
        this.onScreen = onScreen
        calls += "watch fragment screens"
        onWatchFragmentScreens()
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

    override fun trackActivities() {
        activityTrackingStarts++
    }

    override fun tryInitWorkManager() {
        calls += "try init WorkManager"
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

    override fun createDevice(
        deviceId: DeviceId,
        pushToken: String,
        pushSubscribed: Boolean,
        languageCode: String
    ): Device = stubDevice(deviceId, pushToken, pushSubscribed, languageCode)

    override fun systemLocale(): Locale = systemLocale

    override fun appLocale(): Locale = appLocale

    override fun defaultLocale(): Locale = defaultLocale

    override fun likelyRegion(language: String): String = likelyRegions[language].orEmpty()

    override fun watchResumedActivities(onResumed: () -> Unit) {
        calls += "watch resumed activities"
        onActivityResumed = onResumed
        if (activityResumed) onResumed()
    }

    override fun stopWatchingResumedActivities() {
        calls += "stop watching resumed activities"
        onActivityResumed = null
    }

    fun resumeActivity() {
        activityResumed = true
        onActivityResumed?.invoke()
    }

    override fun displayedLocale(): Locale? {
        onDisplayedLocaleRead()
        return displayedLocale
    }

    override fun notificationsEnabled(channelId: String): Boolean {
        notificationChannelsChecked += channelId
        notificationsReadFailure?.let { throw it }
        return notificationsEnabled
    }

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
