package com.reteno.sample

import android.app.Application
import android.os.Handler
import com.reteno.core.Reteno
import com.reteno.core.RetenoApplication
import com.reteno.core.RetenoConfig
import com.reteno.core.RetenoImpl
import com.reteno.core.domain.model.event.LifecycleTrackingOptions.Companion.ALL
import com.reteno.core.identification.DeviceIdProvider
import com.reteno.core.lifecycle.ScreenTrackingConfig
import com.reteno.sample.util.AppSharedPreferencesManager.getDeviceId
import com.reteno.sample.util.AppSharedPreferencesManager.getDeviceIdDelay
import com.reteno.sample.util.AppSharedPreferencesManager.getShouldDelayLaunch
import com.reteno.sample.util.AppSharedPreferencesManager.setDelayLaunch

class DeprecatedApp : Application(), RetenoApplication {
    private var retenoInstance: Reteno? = null
    override fun onCreate() {
        super.onCreate()
        if (getShouldDelayLaunch(this)) {
            setDelayLaunch(this, false)
            val instance = RetenoImpl(this)
            retenoInstance = instance
            val handler = Handler()
            handler.postDelayed({
                instance.initWith(
                    RetenoConfig(
                        false,
                        createProvider(),
                        ALL,
                        BuildConfig.API_ACCESS_KEY
                    )
                )
            }, 3000L)
        } else {
            retenoInstance =
                RetenoImpl(this, BuildConfig.API_ACCESS_KEY, RetenoConfig(false, createProvider()))
        }
        val excludeScreensFromTracking = ArrayList<String>()
        excludeScreensFromTracking.add("NavHostFragment")
        retenoInstance?.autoScreenTracking(ScreenTrackingConfig(false, excludeScreensFromTracking))
    }

    private fun createProvider(): DeviceIdProvider? {
        var provider: DeviceIdProvider? = null
        val deviceIdDelay = getDeviceIdDelay(this)
        val deviceId = getDeviceId(this)
        if (!deviceId!!.isEmpty()) {
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

    override fun getRetenoInstance(): Reteno {
        return retenoInstance!!
    }
}
