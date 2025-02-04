package com.reteno.sample

import android.app.Application
import android.os.Handler
import com.reteno.core.Reteno
import com.reteno.core.RetenoConfig
import com.reteno.core.domain.model.event.LifecycleTrackingOptions.Companion.ALL
import com.reteno.core.identification.DeviceIdProvider
import com.reteno.core.lifecycle.ScreenTrackingConfig
import com.reteno.sample.util.AppSharedPreferencesManager.getDeviceId
import com.reteno.sample.util.AppSharedPreferencesManager.getDeviceIdDelay
import com.reteno.sample.util.AppSharedPreferencesManager.getShouldDelayLaunch
import com.reteno.sample.util.AppSharedPreferencesManager.setDelayLaunch

class SampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (getShouldDelayLaunch(this)) {
            setDelayLaunch(this, false)
            val handler = Handler()
            handler.postDelayed({
                Reteno.initWith(
                    RetenoConfig(
                        accessKey = BuildConfig.API_ACCESS_KEY,
                        isPausedInAppMessages = false,
                        userIdProvider = createProvider(),
                        lifecycleTrackingOptions = ALL
                    )
                )
            }, 3000L)
        } else {
            Reteno.initWith(
                RetenoConfig(
                    accessKey = BuildConfig.API_ACCESS_KEY,
                    isPausedInAppMessages = false,
                    userIdProvider = createProvider(),
                    lifecycleTrackingOptions = ALL
                )
            )
        }
        val excludeScreensFromTracking = ArrayList<String>()
        excludeScreensFromTracking.add("NavHostFragment")
        Reteno.instance.autoScreenTracking(ScreenTrackingConfig(false, excludeScreensFromTracking))
    }

    private fun createProvider(): DeviceIdProvider? {
        var provider: DeviceIdProvider? = null
        val deviceIdDelay = getDeviceIdDelay(this)
        val deviceId = getDeviceId(this)
        if (deviceId!!.isNotEmpty()) {
            val startTime = System.currentTimeMillis()
            provider = DeviceIdProvider {
                if (System.currentTimeMillis() - startTime > deviceIdDelay) {
                    return@DeviceIdProvider deviceId
                } else {
                    return@DeviceIdProvider null
                }
            }
        }
        return provider
    }
}
