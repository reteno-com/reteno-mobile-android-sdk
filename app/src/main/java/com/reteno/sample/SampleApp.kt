package com.reteno.sample

import android.app.Application
import android.os.Handler
import androidx.work.Configuration
import com.reteno.core.Reteno
import com.reteno.core.RetenoConfig
import com.reteno.core.domain.model.event.LifecycleTrackingOptions.Companion.ALL
import com.reteno.core.identification.DeviceIdProvider
import com.reteno.core.lifecycle.ScreenTrackingConfig
import com.reteno.sample.util.AppSharedPreferencesManager.getDeviceId
import com.reteno.sample.util.AppSharedPreferencesManager.getDeviceIdDelay
import com.reteno.sample.util.AppSharedPreferencesManager.getShouldDelayLaunch
import com.reteno.sample.util.AppSharedPreferencesManager.setDelayLaunch

class SampleApp : Application(), Configuration.Provider {

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        if (getShouldDelayLaunch(this)) {
            setDelayLaunch(this, false)
            Handler().postDelayed({
                Reteno.initWithConfig(
                    RetenoConfig.Builder()
                        .pauseInAppMessages(false)
                        .customDeviceIdProvider(createProvider())
                        .lifecycleTrackingOptions(ALL)
                        .accessKey(BuildConfig.API_ACCESS_KEY)
                        .build()
                )
            }, 3000L)
        } else {
            Reteno.initWithConfig(
                RetenoConfig.Builder()
                    .pauseInAppMessages(false)
                    .customDeviceIdProvider(createProvider())
                    .accessKey(BuildConfig.API_ACCESS_KEY)
                    .build()
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
