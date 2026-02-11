package com.reteno.sample

import android.app.Application
import android.widget.Toast
import androidx.work.Configuration
import com.reteno.core.Reteno
import com.reteno.core.RetenoConfig
import com.reteno.core.identification.DeviceIdProvider
import com.reteno.core.lifecycle.ScreenTrackingConfig
import com.reteno.core.util.toStringVerbose
import com.reteno.push.RetenoNotifications
import com.reteno.sample.util.AppSharedPreferencesManager.getDeviceId
import com.reteno.sample.util.AppSharedPreferencesManager.getDeviceIdDelay
import com.reteno.sample.util.AppSharedPreferencesManager.getShouldDelayLaunch

class SampleApp : Application(), Configuration.Provider {

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        if (!getShouldDelayLaunch(this)) {
            Reteno.initWithConfig(
                RetenoConfig.Builder()
                    .pauseInAppMessages(false)
                    .customDeviceIdProvider(createProvider())
                    .accessKey(BuildConfig.API_ACCESS_KEY)
                    .setDebug(BuildConfig.DEBUG)
                    .build()
            )
        }
        val excludeScreensFromTracking = ArrayList<String>()
        excludeScreensFromTracking.add("NavHostFragment")
        Reteno.instance.autoScreenTracking(ScreenTrackingConfig(false, excludeScreensFromTracking))
        RetenoNotifications.click.addListener {
            val text = "Push click: ${it.toStringVerbose()}"
            Toast.makeText(this, text, Toast.LENGTH_SHORT)
                .show()
        }
        RetenoNotifications.custom.addListener {
            val text = "Custom push received: ${it.toStringVerbose()}"
            Toast.makeText(this, text, Toast.LENGTH_SHORT)
                .show()
        }
        RetenoNotifications.close.addListener {
            val text = "Push closed: ${it.toStringVerbose()}"
            Toast.makeText(this, text, Toast.LENGTH_SHORT)
                .show()
        }
        RetenoNotifications.received.addListener {
            val text = "Push received: ${it.toStringVerbose()}"
            Toast.makeText(this, text, Toast.LENGTH_SHORT)
                .show()
        }
        RetenoNotifications.inAppCustomDataReceived.addListener {
            val text = "InApp custom data received: $it"
            Toast.makeText(this, text, Toast.LENGTH_SHORT)
                .show()
        }
    }

    fun createProvider(): DeviceIdProvider? {
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
