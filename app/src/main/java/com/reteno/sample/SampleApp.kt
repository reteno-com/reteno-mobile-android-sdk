package com.reteno.sample

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Configuration
import com.reteno.core.Reteno
import com.reteno.core.RetenoConfig
import com.reteno.core.identification.DeviceIdProvider
import com.reteno.core.lifecycle.ScreenTrackingConfig
import com.reteno.core.util.toStringVerbose
import com.reteno.push.RetenoNotifications
import com.reteno.sample.util.AppSharedPreferencesManager
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
                    .lifecycleTrackingOptions(AppSharedPreferencesManager.getOptions(this))
                    .sessionDuration(AppSharedPreferencesManager.getSessionDuration(this))
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
            showSummaryNotification()
        }
        RetenoNotifications.inAppCustomDataReceived.addListener {
            val text = "InApp custom data received: $it"
            Toast.makeText(this, text, Toast.LENGTH_SHORT)
                .show()
        }
        RetenoNotifications.setGroupingRule {
            Log.i("SampleApp", it.toString())
            "test_group"
        }
    }

    private fun showSummaryNotification() {
        val manager = NotificationManagerCompat.from(this)
        if (manager.activeNotifications.count { it.notification.group == "test_group" } < 2)
            return
        val summaryNotification = NotificationCompat.Builder(this, "defaultId")
            .setContentTitle("Summary")
            .setContentText("Two new messages")
            .setSmallIcon(R.drawable.ic_ok)
            .setGroup("test_group")
            .setGroupSummary(true)
            .build()

        if (ActivityCompat.checkSelfPermission(
                this@SampleApp,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return
        manager.notify("SUMMARY_ID".hashCode(), summaryNotification)
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
