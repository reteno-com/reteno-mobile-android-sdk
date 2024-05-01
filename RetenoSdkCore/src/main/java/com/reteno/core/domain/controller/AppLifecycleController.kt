package com.reteno.core.domain.controller

import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.MainThread
import com.reteno.core.RetenoImpl
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.domain.model.event.Event
import com.reteno.core.lifecycle.RetenoSessionHandler
import com.reteno.core.util.Logger
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

internal class AppLifecycleController(
    private val configRepository: ConfigRepository,
    private val eventController: EventController,
    private val sessionHandler: RetenoSessionHandler,
    isLifecycleEventTrackingEnabled: Boolean
) {

    private val isLifecycleEventTrackingEnabled = AtomicBoolean(isLifecycleEventTrackingEnabled)
    private var wasBackgrounded = false
    private var appOpenedTimestamp = System.currentTimeMillis()

    @MainThread
    fun start() {
        trackLifecycleEvent(Event.applicationOpen(wasBackgrounded))
        appOpenedTimestamp = System.currentTimeMillis()
        wasBackgrounded = false
    }

    @MainThread
    fun stop() {
        wasBackgrounded = true
        trackLifecycleEvent(
            Event.applicationBackgrounded(
                appOpenedTimestamp.milliseconds.inWholeSeconds,
                sessionHandler.getForegroundTimeMillis().milliseconds.inWholeSeconds
            )
        )
    }

    fun enableLifecycleEvents(isEnabled: Boolean) {
        /*@formatter:off*/ Logger.i(TAG, "enableLifecycleEvents(): ", "isEnabled = [" , isEnabled , "]")
        /*@formatter:on*/
        isLifecycleEventTrackingEnabled.set(isEnabled)
    }

    fun initMetadata() {
        val savedAppVersion = configRepository.getAppVersion()
        val savedAppBuild = configRepository.getAppBuildNumber()
        val (version, code) = try {
            val packageName = RetenoImpl.application.packageName
            val pInfo = RetenoImpl.application.packageManager.getPackageInfo(packageName, 0)
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                pInfo.versionCode.toLong()
            }
            pInfo.versionName to versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            "" to 0L
        }
        when {
            savedAppVersion.isEmpty() -> {
                trackLifecycleEvent(Event.applicationInstall(version, code))
            }

            savedAppVersion != version -> {
                trackLifecycleEvent(
                    Event.applicationUpdate(
                        version = version,
                        build = code,
                        prevVersion = savedAppVersion,
                        prevBuild = savedAppBuild
                    )
                )
                configRepository.saveAppVersion(version)
                configRepository.saveAppBuildNumber(code)
            }
        }
    }

    private fun trackLifecycleEvent(event: Event) {
        if (isLifecycleEventTrackingEnabled.get()) {
            eventController.trackEvent(event)
        }
    }

    companion object {
        private val TAG: String = AppLifecycleController::class.java.simpleName
    }
}